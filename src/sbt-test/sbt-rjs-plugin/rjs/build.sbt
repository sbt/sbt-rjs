lazy val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies ++= Seq(
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "underscorejs" % "1.6.0-1",
  "org.webjars" % "knockout" % "2.3.0"
)

pipelineStages := Seq(rjs)

val checkCdn = taskKey[Unit]("Check the CDN")

checkCdn := {
  if (RjsKeys.paths.value != Map(
    "myunderscore" -> ("lib/underscorejs/underscore", "http://cdn.jsdelivr.net/webjars/underscorejs/1.6.0-1/underscore-min"),
    "myknockout" -> ("lib/knockout/knockout", "http://cdn.jsdelivr.net/webjars/knockout/2.3.0/knockout")
  )) {
    sys.error(s"${RjsKeys.paths.value} is not what we expected")
  }
}
