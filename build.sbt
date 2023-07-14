ThisBuild / scalaVersion := "3.3.0"
ThisBuild / version := "0.1"
ThisBuild / name := "fun"
ThisBuild / organization := "karnicki.eu"

val circeVersion = "0.14.5"
val http4sVersion = "1.0.0-M39"
val scalatestVersion = "3.2.16"
val testcontainersVersion = "0.40.17"

val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % Test
val clientServerLibraries = Seq(
  scalatest +:
    Seq(
      "circe-core",
      "circe-generic")
      .map(artifact => "io.circe" %% artifact % circeVersion)).flatten

val core = (project in file("core")).settings(
  libraryDependencies += scalatest
)
val server = (project in file("server"))
  .settings(
    assembly / mainClass := Some("eu.karnicki.ServerApp"),
    libraryDependencies ++= clientServerLibraries)
  .dependsOn(core)
val client = (project in file("client"))
  .settings(
    assembly / mainClass := Some("eu.karnicki.ClientApp"),
    libraryDependencies ++= (clientServerLibraries ++
      Seq(
        "http4s-ember-server",
        "http4s-circe",
        "http4s-dsl")
        .map(artifact => "org.http4s" %% artifact % http4sVersion)))
  .dependsOn(server)
val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    libraryDependencies ++= Seq(
      "com.dimafeng"  %% "testcontainers-scala-scalatest" % testcontainersVersion % "it",
      "org.scalatest" %% "scalatest"                      % scalatestVersion      % "it"),
    Defaults.itSettings,
    IntegrationTest / fork := true,
    IntegrationTest / parallelExecution := false)
  .aggregate(core, server, client)

