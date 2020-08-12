package org.karlhigley.practicalrecs_janus_etl

import org.janusgraph.core.JanusGraphFactory
import org.janusgraph.core.JanusGraph


object JanusConnection {
  def open(hostName: String, graphName: String): JanusGraph = {
    val graph = JanusGraphFactory.build()
        .set("storage.backend", "cql")
        .set("storage.batch-loading", true)
        .set("storage.hostname", hostName)
        .set("index.search.hostname", hostName)
        .set("storage.cql.keyspace", graphName)
        .open()
    
    if (!graph.tx.isOpen) {
      graph.tx.open()
    }
    
    graph
  }
}
