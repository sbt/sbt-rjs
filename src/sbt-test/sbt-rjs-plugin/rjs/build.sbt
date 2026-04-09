import sbtcompat.PluginCompat
import xsbti.FileConverter

lazy val root = (project in file("."))
  .enablePlugins(SbtWeb)
  .settings(
    // Classic target layout so scripted `$ exists target/web/stage/...` works on sbt 2 too.
    target := baseDirectory.value / "target",

    libraryDependencies ++= Seq(
      "org.webjars" % "requirejs" % "2.1.11-1",
      "org.webjars" % "underscorejs" % "1.6.0-1",
      "org.webjars" % "knockout" % "2.3.0",
      "org.webjars" % "bootstrap" % "3.2.0"
    ),

    pipelineStages := Seq(rjs),
  )

val checkCdn = taskKey[Unit]("Check the CDN")

checkCdn := {
  if (RjsKeys.paths.value != Map(
    "myunderscore" -> ("lib/underscorejs/underscore", "//cdn.jsdelivr.net/webjars/underscorejs/1.6.0-1/underscore-min"),
    "myknockout" -> ("lib/knockout/knockout", "//cdn.jsdelivr.net/webjars/knockout/2.3.0/knockout"),
    "myjquery" -> ("lib/jquery/jquery", "//cdn.jsdelivr.net/webjars/jquery/1.11.1/jquery.min")
  )) {
    sys.error(s"${RjsKeys.paths.value} is not what we expected")
  }
}

// Add an extra file to confuse the thing that finds the main.js mapping (cross sbt 1 / sbt 2).
// sbt2-compat: use implicit FileConverter (sbt 1) / applies to sbt 2 overload too.
Assets / mappings := {
  implicit val conv: FileConverter = fileConverter.value
  val f = baseDirectory.value / "src" / "main" / "foos" / "javascripts" / "main.js.foo"
  (PluginCompat.toFileRef(f), "javascripts/main.js.foo") +: (Assets / mappings).value
}
