package com.typesafe.sbt.rjs

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.{Compat, SbtWeb}
import com.typesafe.sbt.web.SbtWeb.autoImport.WebJs._
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import java.nio.charset.Charset
import scala.collection.immutable.SortedMap
import java.io.{InputStreamReader, BufferedReader}
import com.typesafe.sbt.compat.Sbt10Compat
import Sbt10Compat.SbtIoPath._

object Import {

  val rjs = TaskKey[Pipeline.Stage]("rjs", "Perform RequireJs optimization on the asset pipeline.")

  object RjsKeys {
    val appBuildProfile = TaskKey[JS.Object]("rjs-app-build-profile", "The project build profile contents.")
    val appDir = SettingKey[File]("rjs-app-dir", "The top level directory that contains your app js files. In effect, this is the source folder that rjs reads from.")
    val baseUrl = TaskKey[String]("rjs-base-url", """The dir relative to the source assets or public folder where js files are housed. Will default to "js", "javascripts" or "." with the latter if the other two cannot be found.""")
    val buildProfile = TaskKey[JS.Object]("rjs-build-profile", "Build profile key -> value settings in addition to the defaults supplied by appBuildProfile. Any settings in here will also replace any defaults.")
    val buildWriter = TaskKey[JavaScript]("rjs-build-writer", "The project build writer JavaScript that is responsible for writing out source files in rjs.")
    val dir = SettingKey[File]("rjs-dir", "By default, all modules are located relative to this path. In effect this is the target directory for rjs.")
    val generateSourceMaps = SettingKey[Boolean]("rjs-generate-source-maps", "By default, source maps are generated.")
    val mainConfig = SettingKey[String]("rjs-main-config", "By default, 'main' is used as the module for configuration.")
    val mainConfigFile = TaskKey[File]("rjs-main-config-file", "The full path to the main configuration file.")
    val mainModule = SettingKey[String]("rjs-main-module", "By default, 'main' is used as the module.")
    val modules = SettingKey[Seq[JS.Object]]("rjs-modules", "The json array of modules.")
    val optimize = SettingKey[String]("rjs-optimize", "The name of the optimizer, defaults to uglify2.")
    val paths = TaskKey[Map[String, (String, String)]]("rjs-paths", "RequireJS path mappings of module ids to a tuple of the build path and production path. By default all WebJar libraries are made available from a CDN and their mappings can be found here (unless the cdn is set to None).")
    val preserveLicenseComments = SettingKey[Boolean]("rjs-preserve-license-comments", "Whether to preserve comments or not. Defaults to false given source maps (see http://requirejs.org/docs/errors.html#sourcemapcomments).")
    val removeCombined = SettingKey[Boolean]("rjs-remove-combined", "Whether to remove source files. Defaults to true.")
    val webJarCdns = SettingKey[Map[String, String]]("rjs-webjar-cdns", """CDNs to be used for locating WebJars. By default "org.webjars" is mapped to "jsdelivr".""")
  }

}

object SbtRjs extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport._
  import RjsKeys._

  override def projectSettings = Seq(
    appBuildProfile := getAppBuildProfile.value,
    appDir := (resourceManaged in rjs).value / "appdir",
    baseUrl := getBaseUrl.value,
    buildProfile := JS.Object.empty,
    buildWriter := getBuildWriter.value,
    dir := (resourceManaged in rjs).value / "build",
    excludeFilter in rjs := HiddenFileFilter,
    generateSourceMaps := true,
    includeFilter in rjs := GlobFilter("*.js") | GlobFilter("*.css") | GlobFilter("*.map"),
    mainConfig := mainModule.value,
    mainConfigFile := new File(baseUrl.value, mainConfig.value + ".js"),
    mainModule := "main",
    modules := Seq(JS.Object("name" -> mainModule.value)),
    optimize := "uglify2",
    paths := getWebJarPaths.value,
    preserveLicenseComments := false,
    removeCombined := true,
    resourceManaged in rjs := webTarget.value / rjs.key.label,
    rjs := runOptimizer.dependsOn(webJarsNodeModules in Plugin).value,
    webJarCdns := Map("org.webjars" -> "//cdn.jsdelivr.net/webjars")
  )


  val Utf8 = Charset.forName("UTF-8")

  private def getAppBuildProfile: Def.Initialize[Task[JS.Object]] = Def.task {
    JS.Object(
      "appDir" -> appDir.value,
      "baseUrl" -> baseUrl.value,
      "dir" -> dir.value,
      "generateSourceMaps" -> generateSourceMaps.value,
      "mainConfigFile" -> appDir.value / mainConfigFile.value.getPath,
      "modules" -> modules.value,
      "onBuildWrite" -> buildWriter.value,
      "optimize" -> optimize.value,
      "paths" -> paths.value.map(m => m._1 -> "empty:"),
      "preserveLicenseComments" -> preserveLicenseComments.value,
      "removeCombined" -> removeCombined.value
    ) ++ buildProfile.value
  }

  private def getBaseUrl: Def.Initialize[Task[String]] = Def.task {
    def dirIfExists(dir: String): Option[String] = {
      val dirPath = dir + java.io.File.separator
      if ((mappings in Assets).value.exists(m => m._2.startsWith(dirPath))) {
        Some(dir)
      } else {
        None
      }
    }
    dirIfExists("js").orElse(dirIfExists("javascripts")).getOrElse(".")
  }

  private def getBuildWriter: Def.Initialize[Task[JavaScript]] = Def.task {
    val source = getResourceAsList("buildWriter.js")
      .to[Vector]
      .dropRight(1) :+ s"""})(
          ${JS(unixPath(mainConfigFile.value.toString))},
          ${JS(paths.value.map(e => e._2._1 -> e._2._2))}
          )"""
    JavaScript(source.mkString("\n"))
  }

  private def getResourceAsList(name: String): List[String] = {
    val in = SbtRjs.getClass.getClassLoader.getResourceAsStream(name)
    val reader = new BufferedReader(new InputStreamReader(in, Utf8))
    try {
      IO.readLines(reader)
    } finally {
      reader.close()
    }
  }

  private def getWebJarPaths: Def.Initialize[Task[Map[String, (String, String)]]] = Def.task {
    /*
     * The idea here is to only declare WebJar CDNs where the user has explicitly declared
     * maths in their main configuration js file. The result of this is that rjs will process only
     * what it really needs to process as opposed to an alternate strategy where all WebJar
     * js files are made known to it (which is slow).
     */
    val maybeMainConfigFile = (mappings in Assets).value.find(_._2 == mainConfigFile.value.getPath).map(_._1)
    val webLib = webModulesLib.value
    val webJarsDirectoryValue = (webJarsDirectory in Assets).value
    val webJarsValue = (webJars in Assets).value
    val updateValue = update.value
    maybeMainConfigFile.fold(Map[String, (String, String)]()) { f =>
      val lib = unixPath(withSep(webLib))
      val config = IO.read(f, Utf8)
      val pathModuleMappings = SortedMap(
        s"""['"]?([^\\s'"]*)['"]?\\s*:\\s*[\\[]?.*['"].*/$lib(.*)['"]""".r
          .findAllIn(config)
          .matchData.map(m => m.subgroups(1) -> m.subgroups(0))
          .toIndexedSeq
          : _*
      )
      val webJarLocalPathPrefix = withSep(webJarsDirectoryValue.getPath) + lib
      val webJarRelPaths = webJarsValue.map(f => unixPath(f.getPath.drop(webJarLocalPathPrefix.size))).toSet
      def minifiedModulePath(p: String): String = {
        def ifExists(p: String): Option[String] = if (webJarRelPaths.contains(p + ".js")) Some(p) else None
        ifExists(p + ".min").orElse(ifExists(p + "-min")).getOrElse(p)
      }
      val webJarCdnPaths = for {
        m <- allDependencies(updateValue)
        cdn <- webJarCdns.value.get(m.organization)
      } yield for {
          pm <- pathModuleMappings.from(m.name + "/") if pm._1.startsWith(m.name + "/")
        } yield {
          val (moduleIdPath, moduleId) = pm
          val moduleIdRelPath = minifiedModulePath(moduleIdPath).drop(m.name.size + 1)
          moduleId ->(lib + moduleIdPath, s"$cdn/${m.name}/${m.revision}/$moduleIdRelPath")
        }
      webJarCdnPaths.flatten.toMap
    }
  }

  private def allDependencies(updateReport: UpdateReport): Seq[ModuleID] = {
    updateReport.filter(
      configurationFilter(Compile.name) && artifactFilter(`type` = "jar")
    ).toSeq.map(_._2).distinct
  }

  private def runOptimizer: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    val include = (includeFilter in rjs).value
    val exclude = (excludeFilter in rjs).value
    val appDirValue = appDir.value
    val streamsValue = streams.value
    val dirValue = dir.value
    val targetBuildProfileFile = (resourceManaged in rjs).value / "app.build.js"

    val timeoutPerSourceValue = (timeoutPerSource in rjs).value
    val appBuildProfileValue = appBuildProfile.value
    val webJarsNodeModulesDirectoryValue = (webJarsNodeModulesDirectory in Plugin).value
    val stateValue = state.value
    val engineTypeValue = (engineType in rjs).value
    val commandValue = (command in rjs).value
    mappings =>


      val optimizerMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))
      SbtWeb.syncMappings(
        Compat.cacheStore(streamsValue, "sync-rjs"),
        optimizerMappings,
        appDirValue
      )


      IO.write(targetBuildProfileFile, appBuildProfileValue.js, Utf8)

      val cacheDirectory = streamsValue.cacheDirectory / rjs.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        _ =>
          streamsValue.log.info("Optimizing JavaScript with RequireJS")

          SbtJsTask.executeJs(
            stateValue,
            engineTypeValue,
            commandValue,
            Nil,
            webJarsNodeModulesDirectoryValue / "requirejs" / "bin" / "r.js",
            Seq("-o", targetBuildProfileFile.getAbsolutePath),
            timeoutPerSourceValue * optimizerMappings.size
          )

          Sbt10Compat.allPaths(dirValue).get.toSet
      }

      val optimizedMappings = runUpdate(Sbt10Compat.allPaths(appDirValue).get.toSet).filter(_.isFile).pair(relativeTo(dirValue))
      (mappings.toSet -- optimizerMappings.toSet ++ optimizedMappings).toSeq
  }

  private def withSep(p: String): String = if (p.endsWith(java.io.File.separator)) p else p + java.io.File.separator

  /**
   * Replaces \ -> / so that paths are in UNIX style.
   * Windows understands them too and as a bonus we have no need to escape them in Regex.
   * Also / used URIs and using same separator makes various operations on URIs and paths more reliable.
   * @param p path
   * @return
   */
  private def unixPath(p: String): String = p.replace("\\","/")

}
