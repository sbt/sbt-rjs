package com.typesafe.sbt.compat

import sbt.PathFinder

object Sbt10Compat {
	object DummyPath

	val SbtIoPath = DummyPath

	def allPaths( pathFinder: PathFinder ): PathFinder = {
		return pathFinder.***
	}
}
