sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-rjs"

version := "1.0.8-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.webjars" % "requirejs-node" % "2.1.17"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.1.1")

publishMavenStyle := false

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
  else Some(Classpaths.sbtPluginReleases)
}

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }
