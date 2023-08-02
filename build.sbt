ThisBuild / scalaVersion := "3.3.0"
ThisBuild / version := "0.1"
ThisBuild / name := "fun"
ThisBuild / organization := "karnicki.eu"

val circeVersion = "0.14.5"
val http4sVersion = "1.0.0-M39"
val scalatestVersion = "3.2.16"
val testcontainersVersion = "0.40.17"
val zioVersion = "2.0.15"
val zioHttpVersion = "3.0.0-RC2"

enablePlugins(
  JavaAppPackaging
)
//ThisBuild / assemblyMergeStrategy := {
//  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
//  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
//  case x => MergeStrategy.first
//}

val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % Test
val zioHttp = Seq(
  "dev.zio" %% "zio-http" % zioHttpVersion,
  "dev.zio" %% "zio-http-testkit" % zioHttpVersion % Test)
val clientServerLibraries = Seq(
  (scalatest +: zioHttp) ++ Seq(
    "circe-core",
    "circe-parser",
    "circe-generic")
    .map(artifact => "io.circe" %% artifact % circeVersion)).flatten

val core = (project in file("core")).settings(
  libraryDependencies ++=
    scalatest +:
      Seq("zio", "zio-streams").map(artifact => "dev.zio" %% artifact % zioVersion)
)
val server = (project in file("server"))
  .settings(
    Compile / mainClass := Some("eu.karnicki.fun.CounterpartyServiceApp"),
    Docker / packageName := "local/fun",
    dockerBaseImage := "openjdk:17",
    dockerExposedVolumes := Seq("/opt/docker/.logs", "/opt/docker/.keys"),
    dockerExposedPorts ++= Seq(8080, 8081),
    libraryDependencies ++= clientServerLibraries ++
      Seq(
        "http4s-ember-server",
        "http4s-ember-client",
        "http4s-circe",
        "http4s-dsl")
        .map(artifact => "org.http4s" %% artifact % http4sVersion))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(core)
val client = (project in file("client"))
  .settings(
    Compile / mainClass := Some("eu.karnicki.ClientApp"),
    libraryDependencies ++=
      clientServerLibraries ++
        Seq(
          "zio-test", "zio-test-sbt", "zio-test-magnolia")
          .map(artifact => "dev.zio" %% artifact % zioVersion % Test),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .dependsOn(server)
val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    libraryDependencies ++= Seq(
      "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersVersion % "it",
      "org.scalatest" %% "scalatest" % scalatestVersion % "it",
    ),
    Defaults.itSettings,
    IntegrationTest / fork := true,
    IntegrationTest / parallelExecution := false)
  .aggregate(core, server, client)
  .dependsOn(client)

