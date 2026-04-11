lazy val scala212 = "2.12.21"
lazy val scala3 = "3.8.3"

ThisBuild / crossScalaVersions := Seq(scala212, scala3)
ThisBuild / scalaVersion := scala212

lazy val `sbt-rjs` = (project in file("."))
  .enablePlugins(SbtWebBase)
  .settings(
    description := "Allows RequireJS to be used from within sbt",

    developers += Developer(
      "playframework",
      "The Play Framework Team",
      "contact@playframework.com",
      url("https://github.com/playframework")
    ),

    addSbtJsEngine("1.4.0-M4"),

    libraryDependencies ++= Seq(
      "org.webjars" % "rjs" % "2.3.6"
    ),

    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.12.9"
        case _      => "2.0.0-RC11"
      }
    },

    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "2.12" => Seq("-Xsource:3", "-release:8")
        case _      => Nil
      }
    },

  )

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}
