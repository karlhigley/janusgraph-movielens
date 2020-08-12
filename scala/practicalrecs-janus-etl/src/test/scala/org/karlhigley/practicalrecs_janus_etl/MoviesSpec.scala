package org.karlhigley.practicalrecs_janus_etl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class MoviesSpec extends AnyFlatSpec with Matchers {
  "Title parser" should "extract title and year" in {
    val (title, year) = Movies.parseTitleAndYear(" Toy Story (1995) ")

    title shouldEqual "Toy Story"
    year shouldEqual Some(1995)
  }

  "Genre parser" should "extract genres" in {
    val genres = Movies.parseGenres(" Adventure|  Animation|Children |Comedy|Fantasy    ")

    genres shouldEqual List("Adventure", "Animation", "Children", "Comedy", "Fantasy")
  }

  "Movie parser" should "extract all fields" in {
    val movieCsv = MovieCsv(1, "Toy Story (1995)", "Adventure|Animation|Children|Comedy|Fantasy")

    val movieVertex = Movies.parseMovie(movieCsv)

    movieVertex shouldEqual MovieVertex(
          1, "Toy Story", Some(1995),
          List("Adventure", "Animation", "Children", "Comedy", "Fantasy")
    )

  }
}


