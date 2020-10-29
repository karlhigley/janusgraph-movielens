package org.karlhigley.practicalrecs_janus_etl

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Dataset
import org.janusgraph.core.JanusGraph
import os.Path


object LoadUserRatings extends App {

  // Create Spark session
  val spark: SparkSession = SparkSession
    .builder()
    .appName("Load Movielens ratings")
    .getOrCreate()

  import spark.implicits._

  val movielens: JanusGraph = JanusConnection.open("0.0.0.0", "movielens")
  val mlt = movielens.traversal

  // Load movielens ratings from CSV
  val csvOptions: Map[String,String] = Map("inferSchema"->"true", "delimiter"->",", "header"->"true")
  val datasetPath: Path = os.Path(args(0)) / "ratings.csv"
  val ratings: Dataset[RatingCsv] = spark.read.options(csvOptions).csv(datasetPath.toString()).as[RatingCsv]

  // Create list of unique users in the dataset
  val uniqueUsers: Array[Int] = ratings.map(_.userId).distinct().collect().sorted
  val uniqueMovies: Array[Int] = ratings.map(_.movieId).distinct().collect().sorted

  val userVertexIdPairs: List[(Int, Object)] = uniqueUsers.grouped(100).flatMap(group => {
    val pairs = group.map(u => {
      val userVertex = mlt.addV("USER").property("userId", u).next()
      u -> userVertex.id
    })
    movielens.tx.commit
    pairs
  }).toList
  val userVertexMapping: Map[Int, Object] = Map(userVertexIdPairs: _*)

  val movieVertexIdPairs: Seq[(Int, Object)] = uniqueMovies.map(m => {
    val movieVertex = mlt.V().has("MOVIE", "movieId", m).next()
    m -> movieVertex.id
  })
  val movieVertexMapping: Map[Int, Object] = Map(movieVertexIdPairs: _*)

  movielens.tx.commit
  movielens.close

  // Create rating edges in JanusGraph
  ratings.foreachPartition((ratings: scala.collection.Iterator[RatingCsv]) => {
    val movielens = JanusConnection.open("0.0.0.0", "movielens")
    val mlt = movielens.traversal

    for (group <- ratings.grouped(100)) {
      for (rating <- group) {
        // Look up edge vertices
        val userVertexId = userVertexMapping(rating.userId)
        val movieVertexId = movieVertexMapping(rating.movieId)

        // Connect vertices with rating edge and set properties        
        mlt.V(userVertexId).as("u")
          .V(movieVertexId).addE("RATED").from("u")
          .property("score", rating.rating.toFloat)
          .property("timestamp", rating.timestamp)
          .next()
      }
      movielens.tx.commit
    }

    movielens.close
  })

}
