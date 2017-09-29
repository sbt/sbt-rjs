package com.typesafe.sbt.compat

import sbt.{Def, PathFinder}
import sbt.Keys.streams

object Sbt10Compat {
	val SbtIoPath = sbt.io.Path

	def allPaths( pathFinder: PathFinder ): PathFinder = {
		return pathFinder.allPaths
	}
}
