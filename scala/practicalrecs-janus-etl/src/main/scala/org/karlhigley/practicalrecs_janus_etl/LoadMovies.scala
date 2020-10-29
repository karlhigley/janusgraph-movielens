package org.karlhigley.practicalrecs_janus_etl

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Dataset
import org.apache.spark.rdd.RDD
import org.janusgraph.core.JanusGraph
import os.Path


object LoadMovies extends App {
  def toInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case _: Exception => None
    }
  }

  def parseTitleAndYear(rawTitle: String): (String, Option[Int]) = {
    val titleAndYearRegex = """(.*)\s+\((\d{4})\)""".r
    val trimmedTitle = rawTitle.trim

    trimmedTitle match {
      case titleAndYearRegex(regexTitle, regexYear) => (regexTitle, toInt(regexYear))
      case _ => (trimmedTitle, None)
    }
  }

  def parseGenres(rawGenres: String): List[String] = {
    rawGenres.split('|').map(_.trim).toList
  }

  def parseMovie(m: MovieCsv): MovieVertex = {
    val (parsedTitle, parsedYear) = parseTitleAndYear(m.title)
    val parsedGenres = parseGenres(m.genres)
    MovieVertex(m.movieId, parsedTitle, parsedYear, parsedGenres)
  }

  // Create Spark session
  val spark: SparkSession = SparkSession
    .builder()
    .appName("Load Movielens movies")
    .getOrCreate()

  import spark.implicits._

  val movielens: JanusGraph = JanusConnection.open("0.0.0.0", "movielens")

  // Load movielens movies from CSV
  val csvOptions: Map[String,String] = Map("inferSchema"->"true", "delimiter"->",","header"->"true")
  val datasetPath: Path = os.Path(args(0)) / "movies.csv"
  val movies: Dataset[MovieCsv] = spark.read.options(csvOptions).csv(datasetPath.toString()).as[MovieCsv]

  val parsedMovies: RDD[MovieVertex] = movies.rdd.map(parseMovie)

  // Create lists of the unique years and genres in the dataset
  val uniqueYears: Array[Int] = parsedMovies.flatMap(_.year).distinct().collect().sorted
  val uniqueGenres: Array[String] = parsedMovies.flatMap(_.genres).distinct().collect().sorted

  val yearVertexIdPairs: Seq[(Int, Object)] = uniqueYears.map(y => {
    val yearVertex = movielens.traversal.addV("YEAR").property("yearNumber", y).next()
    y -> yearVertex.id
  })
  val yearVertexMapping: Map[Int, Object] = Map(yearVertexIdPairs: _*)

  movielens.tx.commit

  val genreVertexIdPairs: Seq[(String, Object)] = uniqueGenres.map(g => {
    val genreVertex = movielens.traversal.addV("GENRE").property("genreName", g).next()
    g -> genreVertex.id
  })
  val genreVertexMapping: Map[String, Object] = Map(genreVertexIdPairs: _*)

  movielens.tx.commit
  movielens.close

  // Create vertices in JanusGraph
  parsedMovies.foreachPartition((movies: scala.collection.Iterator[MovieVertex]) => {
    val movielens = JanusConnection.open("0.0.0.0", "movielens")
    val mlt = movielens.traversal

    for (group <- movies.grouped(100)) {
      for (movie <- group) {
        // Add movie vertex with properties
        val movieVertex = mlt.addV("MOVIE")
            .property("movieId", movie.movieId)
            .property("movieTitle", movie.title)
            .next()

        movie.year.foreach(mlt.V(movieVertex).property("movieYear", _).next())

        if (movie.genres.length > 0) {
          mlt.V(movieVertex).property("movieGenres", movie.genres).next()
        }

        // Add year and genre edges
        movie.year.foreach(y => {
          mlt.V(movieVertex).as("m").V(yearVertexMapping(y)).addE("RELEASED").from("m").next()
        })

        movie.genres.foreach(g => {
          mlt.V(movieVertex).as("m").V(genreVertexMapping(g)).addE("CATEGORIZED").from("m").next()
        })

      }
      movielens.tx.commit
    }

    movielens.close
  })
}
