import sbt.Keys._
import sbt._

val projectVersion = "0.1.0-SNAPSHOT"

val compilerVersion = "2.11.8"

val scalaParserCombinatorsVersion = "1.0.2"
val scalaReflectVersion = compilerVersion
val scalaMacrosParadiseVersion = "2.1.0"
val scalaXmlVersion = "1.0.2"

val akkaVersion = "2.3.15"
val codehausJaninoVersion = "2.7.5"
val jodaConvertVersion = "1.5"
val jodaTimeVersion = "2.3"
val logbackClassicVersion = "1.0.13"
val pi4jVersion = "1.1"
val scalaCsvVersion = "1.2.1"
val scalacticVersion = "2.2.1"
val scalameterVersion = "0.6"
val scalaMockScalaTestSupportVersion = "3.2"
val scalatestVersion = "2.2.1"
val scoptVersion = "3.3.0"
val scallopVersion = "0.9.5"
val shapelessVersion = "2.2.5"
val slf4jApiVersion = "1.7.5"
val teamCityRestClientVersion = "0.1.49"

lazy val commonSettings = Seq(
  organization := "com.bns",

  version := projectVersion,

  scalaVersion := compilerVersion,

  scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature"),

  // Due to this flag value "true" test cases fail in sbt, need to find alternative way.
  packageOptions in(Compile, packageBin) += Package.ManifestAttributes(java.util.jar.Attributes.Name.SEALED -> "false"),

  unmanagedBase := baseDirectory.value / ".." / "lib",

  updateOptions := updateOptions.value.withCachedResolution(true),

  logBuffered := false,

  parallelExecution in Test := false,

  resolvers += Resolver.bintrayRepo("jetbrains", "teamcity-rest-client")
)

lazy val `pinfeather-monitor` = (project in file("pinfeather-monitor")).
  settings(commonSettings: _*).
  settings(
    name := "pinfeather-monitor",
    libraryDependencies ++= Seq(
      "joda-time" % "joda-time" % jodaTimeVersion,
      "org.joda" % "joda-convert" % jodaConvertVersion,
      "org.slf4j" % "slf4j-api" % slf4jApiVersion,
      "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
      "com.github.scopt" %% "scopt" % scoptVersion,
      "org.jetbrains.teamcity" % "teamcity-rest-client" % teamCityRestClientVersion,
      "com.pi4j" % "pi4j-core" % pi4jVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "org.scalatest" % "scalatest_2.11" % scalatestVersion % "test"
    ),

    test in assembly :=
      {},

    assemblyOption in assembly := (assemblyOption in assembly).value.copy(cacheOutput = true)
  )

lazy val pinfeather = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "pinfeather",
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-pinfeather")).
  aggregate(
    `pinfeather-monitor`
  )
