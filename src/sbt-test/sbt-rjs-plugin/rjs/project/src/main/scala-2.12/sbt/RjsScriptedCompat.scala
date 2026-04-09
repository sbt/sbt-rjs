package sbt

import _root_.java.io.{File => JFile}
import _root_.xsbti.FileConverter

/** Scripted mapping source: sbt 1 Assets / mappings use java.io.File. */
object RjsScriptedCompat {
  def toAssetRef(f: JFile)(implicit conv: FileConverter): JFile = f
}
