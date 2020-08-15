package org.karlhigley.practicalrecs_janus_etl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class LoadOmdbPropertiesSpec extends AnyFlatSpec with Matchers {
  "OMDB parser" should "extract fields" in {

    val parsedJson = ujson.read("{\"Title\":\"Toy Story\",\"Year\":\"1995\",\"Rated\":\"G\",\"Released\":\"22 Nov 1995\",\"Runtime\":\"81 min\",\"Genre\":\"Animation, Adventure, Comedy, Family, Fantasy\",\"Director\":\"John Lasseter\",\"Writer\":\"John Lasseter (original story by), Pete Docter (original story by), Andrew Stanton (original story by), Joe Ranft (original story by), Joss Whedon (screenplay by), Andrew Stanton (screenplay by), Joel Cohen (screenplay by), Alec Sokolow (screenplay by)\",\"Actors\":\"Tom Hanks, Tim Allen, Don Rickles, Jim Varney\",\"Plot\":\"A cowboy doll is profoundly threatened and jealous when a new spaceman figure supplants him as top toy in a boy's room.\",\"Language\":\"English\",\"Country\":\"USA\",\"Awards\":\"Nominated for 3 Oscars. Another 27 wins & 20 nominations.\",\"Poster\":\"https://m.media-amazon.com/images/M/MV5BMDU2ZWJlMjktMTRhMy00ZTA5LWEzNDgtYmNmZTEwZTViZWJkXkEyXkFqcGdeQXVyNDQ2OTk4MzI@._V1_SX300.jpg\",\"Ratings\":[{\"Source\":\"Internet Movie Database\",\"Value\":\"8.3/10\"},{\"Source\":\"Rotten Tomatoes\",\"Value\":\"100%\"},{\"Source\":\"Metacritic\",\"Value\":\"95/100\"}],\"Metascore\":\"95\",\"imdbRating\":\"8.3\",\"imdbVotes\":\"860,790\",\"imdbID\":\"tt0114709\",\"Type\":\"movie\",\"DVD\":\"20 Mar 2001\",\"BoxOffice\":\"N/A\",\"Production\":\"Buena Vista\",\"Website\":\"N/A\",\"Response\":\"True\"}")

    val movieOmdb = LoadOmdbProperties.constructMovieOmdb("1", "tt0114709", parsedJson)

    movieOmdb.rated shouldEqual Some("G")
    movieOmdb.directors shouldEqual Some(List("John Lasseter"))
    movieOmdb.writers shouldEqual Some(List("John Lasseter (original story by)", "Pete Docter (original story by)", "Andrew Stanton (original story by)", "Joe Ranft (original story by)", "Joss Whedon (screenplay by)", "Andrew Stanton (screenplay by)", "Joel Cohen (screenplay by)", "Alec Sokolow (screenplay by)"))
    movieOmdb.actors shouldEqual Some(List("Tom Hanks", "Tim Allen", "Don Rickles", "Jim Varney"))
    movieOmdb.languages shouldEqual Some(List("English"))
    movieOmdb.countries shouldEqual Some(List("USA"))
    movieOmdb.boxOffice shouldEqual None
    movieOmdb.metascore shouldEqual Some(95)
    movieOmdb.imdbScore shouldEqual Some(8.3f)
    movieOmdb.imdbVotes shouldEqual Some(860790)
  }

  "OMDB parser" should "have sensible defaults" in {

    val parsedJson = ujson.read("{\"Response\":\"False\"}")

    val movieOmdb = LoadOmdbProperties.constructMovieOmdb("1", "tt0114709", parsedJson)

    movieOmdb.rated shouldEqual None
    movieOmdb.directors shouldEqual None
    movieOmdb.writers shouldEqual None
    movieOmdb.actors shouldEqual None
    movieOmdb.languages shouldEqual None
    movieOmdb.countries shouldEqual None
    movieOmdb.boxOffice shouldEqual None
    movieOmdb.metascore shouldEqual None
    movieOmdb.imdbScore shouldEqual None
    movieOmdb.imdbVotes shouldEqual None
  }
}
