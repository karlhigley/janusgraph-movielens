package org.karlhigley.practicalrecs_janus_etl

import scala.concurrent.Await
import scala.concurrent.duration._

import dispatch._, Defaults._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Dataset
import os.Path


object FetchOmdb extends App {

  // Create Spark session
  val session: SparkSession = SparkSession
    .builder()
    .appName("Enrich Movielens movies with OMDB data")
    .getOrCreate()

  import session.implicits._

  // Read OMDB API key
  val apiKeyPath: Path = os.Path("~/.omdb-api-key")
  val apiKey: String = os.read(apiKeyPath)

  // Load movielens link IDs from CSV
  val csvOptions: Map[String,String] = Map("delimiter"->",","header"->"true")

  val datasetPath: Path = os.Path(args(0)) / "links.csv"
  val linksCsv: Dataset[LinkCsv] = session.read.options(csvOptions)
                        .csv(datasetPath.toString())
                        .as[LinkCsv].repartition(128)
  
  val enrichedMovies: Dataset[OmdbResponse] = linksCsv.mapPartitions((links: scala.collection.Iterator[LinkCsv]) => {
    links.map(l => {
      val omdbEndpoint = url("http://www.omdbapi.com/")
      val authedReq = omdbEndpoint.addQueryParameter("apikey", apiKey)
      val movieRequest = authedReq.addQueryParameter("i", "tt" + l.imdbId)
      
      val getResponse = () => {
        Http.default(movieRequest OK as.String).either.map {
          case Left(e@_) => {
            None
          }
          case Right(response) => {
            Some(response)
          }
        }
      }
      val response = retry.Backoff(max=6, delay=1.seconds, base = 2)(getResponse)

      OmdbResponse(l.movieId, l.imdbId, Await.result(response, 70 seconds))
    })
  }).cache()

  // Persist the scraped data out to disk
  val outputPath: Path = os.Path(args(0)) / "omdb-json"
  enrichedMovies
      .write.format("json")
      .save(outputPath.toString())

  session.stop()
}

