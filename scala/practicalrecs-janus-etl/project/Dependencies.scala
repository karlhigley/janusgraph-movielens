import sbt._

object Dependencies {
  lazy val sparkVersion = "3.0.0"
  lazy val gremlinScalaVersion = "3.4.7.1"
  lazy val gremlinDriverVersion = "3.3.3"
  lazy val janusVersion = "0.3.1"
  lazy val dispatchVersion = "1.2.0"
  lazy val ujsonVersion = "1.2.0"
  lazy val oslibVersion = "0.2.7"
  lazy val scalaTestVersion = "3.2.0"

  lazy val spark = "org.apache.spark" % "spark-core_2.12" % sparkVersion % "provided"
  lazy val sparkSql = "org.apache.spark" % "spark-sql_2.12" % sparkVersion % "provided"
  lazy val gremlinScala = "com.michaelpollmeier" %% "gremlin-scala" % gremlinScalaVersion
  lazy val gremlinDriver = "org.apache.tinkerpop" % "gremlin-driver" % gremlinDriverVersion
  lazy val janusCore = "org.janusgraph" % "janusgraph-core" % janusVersion
  lazy val janusCassandra = "org.janusgraph" % "janusgraph-cassandra" % janusVersion
  lazy val janusCql = "org.janusgraph" % "janusgraph-cql" % janusVersion
  lazy val janusElastic = "org.janusgraph" % "janusgraph-es" % janusVersion
  lazy val dispatch = "org.dispatchhttp" %% "dispatch-core" % dispatchVersion
  lazy val ujson = "com.lihaoyi" %% "ujson" % ujsonVersion
  lazy val oslib = "com.lihaoyi" %% "os-lib" % oslibVersion

  lazy val scalactic = "org.scalactic" %% "scalactic" % scalaTestVersion
  lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test" 
}



