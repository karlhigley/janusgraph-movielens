package org.karlhigley.practicalrecs_janus_etl

import gremlin.scala.{label, underlying, Vertex}

// CSV inputs
final case class MovieCsv(movieId: Int, title: String, genres: String)
final case class RatingCsv(userId: Int, movieId: Int, rating: Double, timestamp: Long)

final case class GenomeTagCsv(tagId: Int, tag: String)
final case class GenomeScoreCsv(movieId: Int, tagId: Int, relevance: Double)

final case class LinkCsv(movieId: String, imdbId: String, tmdbId: Option[String])

// API inputs

case class OmdbResponse(movieId: String, imdbId: String, response: Option[String])

final case class MovieOmdb(
  movieId: String,
  imdbId: String,
  tmdbId: Option[String],
  rated: Option[String],
  directors: Option[List[String]],
  writers: Option[List[String]],
  actors: Option[List[String]],
  languages: Option[List[String]],
  countries: Option[List[String]],
  boxOffice: Option[Float],
  metascore: Option[Float],
  imdbScore: Option[Float],
  imdbVotes: Option[Long]
)

// JanusGraph outputs
@label("MOVIE")
final case class MovieVertex(movieId: Int, title: String, year: Option[Int], genres: Seq[String], @underlying vertex: Option[Vertex] = None)

@label("USER")
final case class UserVertex(userId: Int, @underlying vertex: Option[Vertex] = None)
