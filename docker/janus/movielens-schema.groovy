:remote connect tinkerpop.server conf/remote.yaml session
:remote console

mgmt = movielens.openManagement()

// Vertex labels from Movielens
movie = mgmt.makeVertexLabel("MOVIE").make()
year = mgmt.makeVertexLabel("YEAR").make()
genre = mgmt.makeVertexLabel("GENRE").make()
tag = mgmt.makeVertexLabel("TAG").make()  
user = mgmt.makeVertexLabel("USER").make()

// Vertex labels from OMDB
director = mgmt.makeVertexLabel("DIRECTOR").make()
writer = mgmt.makeVertexLabel("WRITER").make()
actor = mgmt.makeVertexLabel("ACTOR").make()
language = mgmt.makeVertexLabel("LANGUAGE").make()
country = mgmt.makeVertexLabel("COUNTRY").make()

// Edge labels from Movielens
released = mgmt.makeEdgeLabel("RELEASED").make()
categorized = mgmt.makeEdgeLabel("CATEGORIZED").make()
labeled = mgmt.makeEdgeLabel("TAGGED").make()
estimated = mgmt.makeEdgeLabel("ESTIMATED").make()
rated = mgmt.makeEdgeLabel("RATED").make()

// Edge labels from OMDB
directed = mgmt.makeEdgeLabel("DIRECTED").make()
wrote = mgmt.makeEdgeLabel("WROTE").make()
acted = mgmt.makeEdgeLabel("ACTED").make()
spoken = mgmt.makeEdgeLabel("SPOKEN").make()
produced = mgmt.makeEdgeLabel("PRODUCED").make()

// Vertex properties from Movielens
movieId = mgmt.makePropertyKey("movieId").dataType(Long.class).make()
movieImdbId = mgmt.makePropertyKey("movieImdbId").dataType(String.class).make()
movieTmdbId = mgmt.makePropertyKey("movieTmdbId").dataType(Long.class).make()
movieTitle = mgmt.makePropertyKey("movieTitle").dataType(String.class).make()
movieYear = mgmt.makePropertyKey("movieYear").dataType(Long.class).make()
movieGenres = mgmt.makePropertyKey("movieGenres").dataType(String.class).cardinality(SET).make()

yearNumber = mgmt.makePropertyKey("yearNumber").dataType(Long.class).make()

genreName = mgmt.makePropertyKey("genreName").dataType(String.class).make()

tagId = mgmt.makePropertyKey("tagId").dataType(Long.class).make()
tagName = mgmt.makePropertyKey("tagName").dataType(String.class).make()

userId = mgmt.makePropertyKey("userId").dataType(Long.class).make()

// Vertex properties from OMDB
movieRated = mgmt.makePropertyKey("movieRated").dataType(String.class).make()
movieBoxOffice = mgmt.makePropertyKey("movieBoxOffice").dataType(Float.class).make()
movieMetascore = mgmt.makePropertyKey("movieMetascore").dataType(Float.class).make()
movieImdbScore = mgmt.makePropertyKey("movieImdbScore").dataType(Float.class).make()
movieImdbVotes = mgmt.makePropertyKey("movieImdbVotes").dataType(Long.class).make()

directorName = mgmt.makePropertyKey("directorName").dataType(String.class).make()
writerName = mgmt.makePropertyKey("writerName").dataType(String.class).make()
actorName = mgmt.makePropertyKey("actorName").dataType(String.class).make()
languageName = mgmt.makePropertyKey("languageName").dataType(String.class).make()
countryName = mgmt.makePropertyKey("countryName").dataType(String.class).make()

// Edge properties from Movielens
score = mgmt.makePropertyKey("score").dataType(Float.class).make()
timestamp = mgmt.makePropertyKey("timestamp").dataType(Long.class).make()

relevance = mgmt.makePropertyKey("relevance").dataType(Float.class).make()

// Add properties to vertex types
mgmt.addProperties(movie, movieId, movieImdbId, movieTmdbId, movieTitle, movieYear, \
                    movieRated, movieGenres, movieBoxOffice, movieMetascore, \
                    movieImdbScore, movieImdbVotes)

mgmt.addProperties(year, yearNumber)
mgmt.addProperties(genre, genreName)
mgmt.addProperties(tag, tagId, tagName)
mgmt.addProperties(user, userId)
mgmt.addProperties(director, directorName)
mgmt.addProperties(writer, writerName)
mgmt.addProperties(actor, actorName)
mgmt.addProperties(language, languageName)
mgmt.addProperties(country, countryName)

// Add properties to edge types
mgmt.addProperties(rated, score, timestamp)
mgmt.addProperties(estimated, relevance)

// Build composite indices for Movielens properties
mgmt.buildIndex('byMovieIdComposite', Vertex.class).addKey(movieId).unique().indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byMovieImdbIdComposite', Vertex.class).addKey(movieImdbId).unique().indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byMovieTmdbIdComposite', Vertex.class).addKey(movieTmdbId).unique().indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byMovieTitleComposite', Vertex.class).addKey(movieTitle).indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byMovieYearComposite', Vertex.class).addKey(movieYear).indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byMovieGenresComposite', Vertex.class).addKey(movieGenres).indexOnly(movie).buildCompositeIndex()

mgmt.buildIndex('byYearNumberComposite', Vertex.class).addKey(yearNumber).unique().indexOnly(year).buildCompositeIndex()
mgmt.buildIndex('byGenreNameComposite', Vertex.class).addKey(genreName).unique().indexOnly(genre).buildCompositeIndex()
mgmt.buildIndex('byTagIdComposite', Vertex.class).addKey(tagId).unique().indexOnly(tag).buildCompositeIndex()
mgmt.buildIndex('byTagNameComposite', Vertex.class).addKey(tagName).unique().indexOnly(tag).buildCompositeIndex()
mgmt.buildIndex('byUserIdComposite', Vertex.class).addKey(userId).unique().indexOnly(user).buildCompositeIndex()

mgmt.buildIndex('byRatedScoreComposite', Edge.class).addKey(score).indexOnly(rated).buildCompositeIndex()

// Build composite indices for OMDB properties
mgmt.buildIndex('byMovieRatedComposite', Vertex.class).addKey(movieRated).indexOnly(movie).buildCompositeIndex()

mgmt.buildIndex('byDirectorNameComposite', Vertex.class).addKey(directorName).unique().indexOnly(director).buildCompositeIndex()
mgmt.buildIndex('byWriterNameComposite', Vertex.class).addKey(writerName).unique().indexOnly(writer).buildCompositeIndex()
mgmt.buildIndex('byActorNameComposite', Vertex.class).addKey(actorName).unique().indexOnly(actor).buildCompositeIndex()
mgmt.buildIndex('byLanguageNameComposite', Vertex.class).addKey(languageName).unique().indexOnly(language).buildCompositeIndex()
mgmt.buildIndex('byCountryNameComposite', Vertex.class).addKey(countryName).unique().indexOnly(country).buildCompositeIndex()

// Build mixed indices

// TODO: Update list of movie properties
mgmt.buildIndex('byMoviePropertiesMixed', Vertex.class)\
    .addKey(movieId).addKey(movieTitle).addKey(movieYear).addKey(movieGenres).addKey(movieImdbId).addKey(movieTmdbId)\
    .addKey(movieRated).addKey(movieBoxOffice).addKey(movieMetascore).addKey(movieImdbScore).addKey(movieImdbVotes)\
    .indexOnly(movie).buildMixedIndex("search")

mgmt.buildIndex('byTagPropertiesMixed', Vertex.class).addKey(tagId).addKey(tagName).indexOnly(tag).buildMixedIndex("search")

mgmt.buildIndex('byRatedScoreMixed', Edge.class).addKey(score).addKey(timestamp).indexOnly(rated).buildMixedIndex("search")
mgmt.buildIndex('byEstimatedRelevanceMixed', Edge.class).addKey(relevance).indexOnly(estimated).buildMixedIndex("search")

// Print the schema
mgmt.printSchema() 

// Commit and close resources
mgmt.commit()
mgmt.close()
