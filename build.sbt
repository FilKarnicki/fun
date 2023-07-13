ThisBuild / scalaVersion := "3.3.0"
ThisBuild / version := "0.1"
ThisBuild / name := "fun"
ThisBuild / organization := "karnicki.eu"

val circeVersion = "0.14.5"
val http4sVersion = "1.0.0-M39"
val scalatestVersion = "3.2.16"

val commonLibraries = Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion % Test +:
    Seq(
      "circe-core",
      "circe-generic")
      .map(artifact => "io.circe" %% artifact % circeVersion)).flatten

val server = (project in file("server")).settings(
  assembly / mainClass := Some("eu.karnicki.ServerApp"),
  libraryDependencies ++= commonLibraries
)
val client = (project in file("client")).settings(
  assembly / mainClass := Some("eu.karnicki.ClientApp"),
  libraryDependencies ++= (commonLibraries ++
    Seq(
      "http4s-ember-server",
      "http4s-circe",
      "http4s-dsl")
      .map(artifact => "org.http4s" %% artifact % http4sVersion))
).dependsOn(server)
val root = (project in file(".")).aggregate(server, client)


