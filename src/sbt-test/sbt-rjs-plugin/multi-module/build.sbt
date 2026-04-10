lazy val main = (project in file("."))
  .enablePlugins(SbtWeb)
  .dependsOn(a)
  .settings(
    target := baseDirectory.value / "target",
    libraryDependencies += "org.webjars" % "requirejs" % "2.1.11-1",
    pipelineStages := Seq(rjs)
  )

lazy val a = (project in file("modules/a"))
  .enablePlugins(SbtWeb)
  .dependsOn(b)
  .settings(target := baseDirectory.value / "target")

lazy val b = (project in file("modules/b"))
  .enablePlugins(SbtWeb)
  .settings(target := baseDirectory.value / "target")
