# janusgraph-movielens

## Requirements

* Apache Spark 3.0.0+
* Docker
* SBT


## Movie Graph

The movie graph is built with JanusGraph (backed by Cassandra and ElasticSearch) running in Docker containers.

### Setting Up JanusGraph

```
cd docker/janus
docker-compose up
```
Wait for the start up log lines to slow down. You should see a log line like this when it's ready:
```
[gremlin-server-boss-1] INFO  org.apache.tinkerpop.gremlin.server.GremlinServer  - Channel started at port 8182.
```

Open a new terminal window:
```
docker-compose -f docker-compose.yml run --rm -e GREMLIN_REMOTE_HOSTS=janusgraph janusgraph ./bin/gremlin.sh
```

Once the Gremlin console loads, paste in the contents of `docker/janus/movielens-schema.groovy`.

### Movielens Ingestion Jobs

There are 3 Spark jobs in the Scala package that load Movielens data into Janusgraph:

* LoadMovies - loads movie information from Movielens dataset
* LoadUserRatings - loads user ratings of movies
* LoadGenomeTags - loads Movielens genome tags and estimated tag relevance scores for each movie

And two (optional) jobs that load additional movie information from the OMDB API:

* FetchOmdbData - scrapes additional movie properties
* LoadOmdbProperties - loads additional movie properties

To run the OMDB jobs, you'll need an [API key](https://www.omdbapi.com/apikey.aspx).

### Running MovieLens Ingestion Jobs

Depending on which parts of the Movielens dataset you're interested in, you can run more or less of the ETL jobs. First, build a JAR from `scala/practicalrecs-janus-etl` with:

```
sbt assembly
```

Then, invoke each job with:
```
spark-submit --class org.karlhigley.practicalrecs_janus_etl.<JobClass> target/scala-2.12/practicalrecs-janus-etl-assembly-0.1.0-SNAPSHOT.jar </path/to/movielens/dataset>
```

(filling in the appropriate class name and dataset path.)
