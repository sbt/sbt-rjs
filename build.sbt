lazy val `sbt-rjs` = project in file(".")
description := "Allows RequireJS to be used from within sbt"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.webjars" % "rjs" % "2.3.6"
)

addSbtJsEngine("1.2.2")
