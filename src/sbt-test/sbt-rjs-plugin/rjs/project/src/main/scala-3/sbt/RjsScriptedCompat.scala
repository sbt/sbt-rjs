package sbt

import _root_.java.io.{File => JFile}
import _root_.xsbti.FileConverter
import _root_.xsbti.HashedVirtualFileRef

/** Scripted mapping source: sbt 2 Assets / mappings use HashedVirtualFileRef. */
object RjsScriptedCompat {
  def toAssetRef(f: JFile)(implicit conv: FileConverter): HashedVirtualFileRef =
    conv.toVirtualFile(f.toPath)
}
