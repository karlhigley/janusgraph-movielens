package org.karlhigley.practicalrecs_janus_etl

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Dataset
import org.janusgraph.core.JanusGraph
import os.Path

object LoadGenomeTags extends App {
  
  // Create Spark session
  val spark: SparkSession = SparkSession
    .builder()
    .appName("Load Movielens genome tags and scores")
    .getOrCreate()

  import spark.implicits._

  val movielens: JanusGraph = JanusConnection.open("0.0.0.0", "movielens")
  val mlt = movielens.traversal

  // Load movielens genome tags from CSV
  val csvOptions: Map[String,String] = Map("inferSchema"->"true", "delimiter"->",","header"->"true")
  val tagsPath: Path = os.Path(args(0)) / "genome-tags.csv"
  val scoresPath: Path = os.Path(args(0)) / "genome-scores.csv"
  val genomeTags: Dataset[GenomeTagCsv] = spark.read.options(csvOptions).csv(tagsPath.toString()).as[GenomeTagCsv].repartition(256)
  val genomeScores: Dataset[GenomeScoreCsv] = spark.read.options(csvOptions).csv(scoresPath.toString()).as[GenomeScoreCsv].repartition(256)

  // Create list of unique tags and movies in the dataset
  val tagVertexIdPairs: List[(Int, Object)] = genomeTags.collect().grouped(100).flatMap(group => {
    val pairs = group.map(t => {
      val tagVertex = mlt.addV("TAG").property("tagId", t.tagId).property("tagName", t.tag).next()
      t.tagId -> tagVertex.id
    })
    movielens.tx.commit
    pairs
  }).toList
  val tagVertexMapping: Map[Int, Object] = Map(tagVertexIdPairs: _*)

  val uniqueMovies: Array[Int] = genomeScores.map(_.movieId).distinct().collect()

  val movieVertexIdPairs: Seq[(Int, Object)] = uniqueMovies.map(m => {
    val movieVertex = mlt.V().has("MOVIE", "movieId", m).next()
    m -> movieVertex.id
  })
  val movieVertexMapping: Map[Int, Object] = Map(movieVertexIdPairs: _*)

  movielens.tx.commit
  movielens.close

  // Create estimated relevance edges in JanusGraph
  genomeScores.foreachPartition((scores: scala.collection.Iterator[GenomeScoreCsv]) => {
    val movielens = JanusConnection.open("0.0.0.0", "movielens")
    val mlt = movielens.traversal

    for (group <- scores.grouped(100)) {
      for (score <- group) {
        // Look up edge vertices
        val tagVertexId = tagVertexMapping(score.tagId)
        val movieVertexId = movieVertexMapping(score.movieId)

        // Connect vertices with estimated relevance edge and set properties        
        mlt.V(tagVertexId).as("t")
          .V(movieVertexId).addE("ESTIMATED_AT").from("t")
          .property("relevance", score.relevance.toFloat)
          .next()
      }
      movielens.tx.commit
    }

    movielens.close
  })
}
