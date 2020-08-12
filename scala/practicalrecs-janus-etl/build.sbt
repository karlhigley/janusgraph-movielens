import Dependencies._

ThisBuild / scalaVersion     := "2.12.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.karlhigley"
ThisBuild / organizationName := "karlhigley"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.0"

lazy val root = (project in file("."))
  .settings(
    name := "practicalrecs-janus-etl",
    libraryDependencies += spark,
    libraryDependencies += sparkSql,
    libraryDependencies += gremlinScala,
    libraryDependencies += gremlinDriver,
    libraryDependencies += janusCore,
    libraryDependencies += janusCassandra,
    libraryDependencies += janusCql,
    libraryDependencies += janusElastic,
    libraryDependencies += ujson,
    libraryDependencies += dispatch,
    libraryDependencies += oslib,
    libraryDependencies += scalaTest
  )

inThisBuild(
  List(
    scalaVersion := "2.12.12",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

scalacOptions ++= Seq(
    "-Ywarn-unused",
    "-Xlint:unused"
)

assemblyShadeRules in assembly := Seq(ShadeRule.rename("com.google.**" -> "shaded.com.google.@1").inAll)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
