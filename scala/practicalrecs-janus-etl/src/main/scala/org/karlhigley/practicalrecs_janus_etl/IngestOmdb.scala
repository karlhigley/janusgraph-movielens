package org.karlhigley.practicalrecs_janus_etl

import scala.util.Try

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Dataset
import org.janusgraph.core.JanusGraph
import org.apache.spark.util.LongAccumulator
import os.Path

object IngestOmdb extends App {

  def parseCommaSeparatedList(csl: String, separator: Char = ','): List[String] = {
    csl.split(separator).map(_.trim).toList
  }

  def buildVertexMapping(graph: JanusGraph, names: List[String], nodeType: String, propertyName: String): Map[String, Object] = {
    val vertexIdPairs = names.grouped(10).flatMap(group => {
      val pairs = group.map(n => {
        val vertex = graph.traversal().addV(nodeType).property(propertyName, n).next()
        n -> vertex.id
      })
      graph.tx.commit
      pairs
    }).toList
    Map(vertexIdPairs: _*)
  }

  def constructMovieOmdb(movieId: String, imdbId: String, parsed: ujson.Value): MovieOmdb = {
    val rated = Try(parsed("Rated").str).toOption

    val directors = Try(parseCommaSeparatedList(parsed("Director").str)).toOption
    val writers = Try(parseCommaSeparatedList(parsed("Writer").str)).toOption
    val actors = Try(parseCommaSeparatedList(parsed("Actors").str)).toOption

    val languages = Try(parseCommaSeparatedList(parsed("Language").str)).toOption
    val countries = Try(parseCommaSeparatedList(parsed("Country").str)).toOption

    val boxOffice = Try(parsed("BoxOffice").str.replaceAll("[^\\d.]+", "").toFloat).toOption

    val metascore = Try(parsed("Metascore").str.toFloat).toOption
    val imdbScore = Try(parsed("imdbRating").str.toFloat).toOption
    val imdbVotes = Try(parsed("imdbVotes").str.replaceAll("[^\\d.]+", "").toLong).toOption

    MovieOmdb(
      movieId, imdbId, "", rated,
      directors, writers, actors, languages, countries,
      boxOffice, metascore, imdbScore, imdbVotes
    )
  }

  // Create Spark session
  val session: SparkSession = SparkSession
    .builder()
    .appName("Enrich Movielens movies with OMDB data")
    .getOrCreate()

  import session.implicits._

  val movielens: JanusGraph = JanusConnection.open("0.0.0.0", "movielens")
  val mlt = movielens.traversal

  // Load movielens link IDs from CSV
  val jsonOptions: Map[String,String] = Map("inferSchema"->"true")

  val datasetPath: Path = os.Path(args(0)) / "omdb-json"
  val omdbJson: Dataset[OmdbResponse] =
      session.read.options(jsonOptions).json(datasetPath.toString()).as[OmdbResponse].coalesce(16)

  val parsingSuccesses: LongAccumulator = session.sparkContext.longAccumulator("Parsing successes")
  val parsingFailures: LongAccumulator = session.sparkContext.longAccumulator("Parsing failures")

  val enrichedMovies: Dataset[MovieOmdb] = omdbJson.flatMap((omdb: OmdbResponse) => {
    omdb.response.flatMap( response => {
      val parsedResponse = ujson.read(response)

      parsedResponse("Response") match {
        case ujson.Str("False") => {
          parsingFailures.add(1)
          None
        }
        case _ => {
          parsingSuccesses.add(1)
          val parsedMovie = constructMovieOmdb(omdb.movieId, omdb.imdbId, parsedResponse)
          Some(parsedMovie)
        }
      }
    })
  }).cache()

  // Create and map vertices for directors, writers, actors, languages, and countries
  val directorsCount: LongAccumulator = session.sparkContext.longAccumulator("Distinct directors")
  val writersCount: LongAccumulator = session.sparkContext.longAccumulator("Distinct writers")
  val actorsCount: LongAccumulator = session.sparkContext.longAccumulator("Distinct actors")
  val languagesCount: LongAccumulator = session.sparkContext.longAccumulator("Distinct languages")
  val countriesCount: LongAccumulator = session.sparkContext.longAccumulator("Distinct countries")

  val directors: List[String] = enrichedMovies.flatMap(_.directors).flatMap(d => d).distinct().collect().toList
  val writers: List[String] = enrichedMovies.flatMap(_.writers).flatMap(w => w).distinct().collect().toList
  val actors: List[String] = enrichedMovies.flatMap(_.actors).flatMap(a => a).distinct().collect().toList
  val languages: List[String] = enrichedMovies.flatMap(_.languages).flatMap(l => l).distinct().collect().toList
  val countries: List[String] = enrichedMovies.flatMap(_.countries).flatMap(c => c).distinct().collect().toList

  directorsCount.add(directors.size)
  writersCount.add(writers.size)
  actorsCount.add(actors.size)
  languagesCount.add(languages.size)
  countriesCount.add(countries.size)  

  val directorVertexMapping: Map[String,Object] = buildVertexMapping(movielens, directors, "DIRECTOR", "directorName")
  val writerVertexMapping: Map[String,Object] = buildVertexMapping(movielens, writers, "WRITER", "writerName")
  val actorVertexMapping: Map[String,Object] = buildVertexMapping(movielens, actors, "ACTOR", "actorName")
  val languageVertexMapping: Map[String,Object] = buildVertexMapping(movielens, languages, "LANGUAGE", "languageName")
  val countryVertexMapping: Map[String,Object] = buildVertexMapping(movielens, countries, "COUNTRY", "countryName")

  movielens.tx.commit
  movielens.close

  // Add properties in JanusGraph
  enrichedMovies.foreachPartition((movies: scala.collection.Iterator[MovieOmdb]) => {
    val movielens = JanusConnection.open("0.0.0.0", "movielens")
    val mlt = movielens.traversal

    for (group <- movies.grouped(100)) {
      for (movie <- group) {
        // Find movie vertex and add properties
        val movieVertex = mlt.V()
              .has("MOVIE", "movieId", movie.movieId)
              .property("movieImdbId", movie.imdbId).next()

        movie.rated.map(mlt.V(movieVertex.id).property("movieRated", _).next())
        movie.boxOffice.map(mlt.V(movieVertex.id).property("movieBoxOffice", _).next())
        movie.metascore.map(mlt.V(movieVertex.id).property("movieMetascore", _).next())
        movie.imdbScore.map(mlt.V(movieVertex.id).property("movieImdbScore", _).next())
        movie.imdbVotes.map(mlt.V(movieVertex.id).property("movieImdbVotes", _).next())

        // Add properties and corresponding edges to other related entities
        for (directors <- movie.directors) {
          for (d <- directors) {
            mlt.V(movieVertex.id).as("m").V(directorVertexMapping(d)).addE("DIRECTED").from("m").next()
          }
        }
        
        for (writers <- movie.writers) {
          for (w <- writers) {
            mlt.V(movieVertex.id).as("m").V(writerVertexMapping(w)).addE("WROTE").from("m").next()
          }
        }

        for (actors <- movie.actors) {
          for (a <- actors) {
            mlt.V(movieVertex.id).as("m").V(actorVertexMapping(a)).addE("ACTED").from("m").next()
          }
        }

        for (languages <- movie.languages) {
          for (l <- languages) {
            mlt.V(movieVertex.id).as("m").V(languageVertexMapping(l)).addE("SPOKEN").from("m").next()
          }
        }

        for (countries <- movie.countries) {
          for (c <- countries) {
            mlt.V(movieVertex.id).as("m").V(countryVertexMapping(c)).addE("PRODUCED").from("m").next()
          }
        }

        movielens.tx.commit
      }
    }

    movielens.close
  })

  println("Parsing successes: " + parsingSuccesses.value)
  println("Parsing failures: " + parsingFailures.value)
  
  println("Distinct directors: " + directorsCount.value)
  println("Distinct writers: " + writersCount.value)
  println("Distinct actors: " + actorsCount.value)
  println("Distinct languages: " + languagesCount.value)
  println("Distinct countries: " + countriesCount.value)
}

