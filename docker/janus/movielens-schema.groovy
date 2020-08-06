:remote connect tinkerpop.server conf/remote.yaml session
:remote console

mgmt = movielens.openManagement()

// Vertex labels
movie = mgmt.makeVertexLabel("MOVIE").make()
year = mgmt.makeVertexLabel("YEAR").make()
genre = mgmt.makeVertexLabel("GENRE").make()
tag = mgmt.makeVertexLabel("TAG").make()  
user = mgmt.makeVertexLabel("USER").make()

// Edge labels
released = mgmt.makeEdgeLabel("RELEASED").make()
categorized = mgmt.makeEdgeLabel("CATEGORIZED").make()
labeled = mgmt.makeEdgeLabel("TAGGED").make()
estimated = mgmt.makeEdgeLabel("ESTIMATED").make()
rated = mgmt.makeEdgeLabel("RATED").make()

// Vertex properties
movieId = mgmt.makePropertyKey("movieId").dataType(Long.class).make()
movieImdbId = mgmt.makePropertyKey("movieImdbId").dataType(Long.class).make()
movieTmdbId = mgmt.makePropertyKey("movieTmdbId").dataType(Long.class).make()
movieTitle = mgmt.makePropertyKey("movieTitle").dataType(String.class).make()
movieYear = mgmt.makePropertyKey("movieYear").dataType(Long.class).make()
movieGenres = mgmt.makePropertyKey("movieGenres").dataType(String.class).cardinality(SET).make()

yearNumber = mgmt.makePropertyKey("yearNumber").dataType(Long.class).make()

genreName = mgmt.makePropertyKey("genreName").dataType(String.class).make()

tagId = mgmt.makePropertyKey("tagId").dataType(Long.class).make()
tagName = mgmt.makePropertyKey("tagName").dataType(String.class).make()

userId = mgmt.makePropertyKey("userId").dataType(Long.class).make()

// Edge properties
score = mgmt.makePropertyKey("score").dataType(Float.class).make()
timestamp = mgmt.makePropertyKey("timestamp").dataType(Long.class).make()

relevance = mgmt.makePropertyKey("relevance").dataType(Float.class).make()

// Add properties to vertex types
mgmt.addProperties(movie, movieId, movieImdbId, movieTmdbId, movieTitle, movieYear, movieGenres)
mgmt.addProperties(year, yearNumber)
mgmt.addProperties(genre, genreName)
mgmt.addProperties(tag, tagId, tagName)
mgmt.addProperties(user, userId)

// Add properties to edge types
mgmt.addProperties(rated, score, timestamp)
mgmt.addProperties(estimated, relevance)

// Build composite indices
mgmt.buildIndex('byMovieIdComposite', Vertex.class).addKey(movieId).unique().indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byMovieTitleComposite', Vertex.class).addKey(movieTitle).indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byMovieYearComposite', Vertex.class).addKey(movieYear).indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byMovieGenresComposite', Vertex.class).addKey(movieGenres).indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byImdbIdComposite', Vertex.class).addKey(movieImdbId).unique().indexOnly(movie).buildCompositeIndex()
mgmt.buildIndex('byTmdbIdComposite', Vertex.class).addKey(movieTmdbId).unique().indexOnly(movie).buildCompositeIndex()

mgmt.buildIndex('byYearNumberComposite', Vertex.class).addKey(yearNumber).unique().indexOnly(year).buildCompositeIndex()
mgmt.buildIndex('byGenreNameComposite', Vertex.class).addKey(genreName).unique().indexOnly(genre).buildCompositeIndex()
mgmt.buildIndex('byTagIdComposite', Vertex.class).addKey(tagId).unique().indexOnly(tag).buildCompositeIndex()
mgmt.buildIndex('byUserIdComposite', Vertex.class).addKey(userId).unique().indexOnly(user).buildCompositeIndex()

mgmt.buildIndex('byRatedScoreComposite', Edge.class).addKey(score).indexOnly(rated).buildCompositeIndex()

// Build mixed indices
mgmt.buildIndex('byMoviePropertiesMixed', Vertex.class).addKey(movieId).addKey(movieTitle).addKey(movieYear).addKey(movieGenres).addKey(movieImdbId).addKey(movieTmdbId).indexOnly(movie).buildMixedIndex("search")
mgmt.buildIndex('byGenrePropertiesMixed', Vertex.class).addKey(genreName).indexOnly(genre).buildMixedIndex("search")
mgmt.buildIndex('byTagPropertiesMixed', Vertex.class).addKey(tagId).addKey(tagName).indexOnly(tag).buildMixedIndex("search")

mgmt.buildIndex('byRatedScoreMixed', Edge.class).addKey(score).addKey(timestamp).indexOnly(rated).buildMixedIndex("search")
mgmt.buildIndex('byEstimatedRelevanceMixed', Edge.class).addKey(relevance).indexOnly(estimated).buildMixedIndex("search")

// Print the schema
mgmt.printSchema() 

// Commit and close resources
mgmt.commit()
mgmt.close()
