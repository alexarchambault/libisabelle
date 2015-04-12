import sbt._
import Keys._

import sbtassembly._

object build extends Build {

  lazy val standardSettings = Seq(
    organization := "cs.tum.edu.isabelle",
    scalaVersion := "2.11.6",
    javacOptions += "-Xlint:unchecked"
  )

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = standardSettings,
    aggregate = Seq(pideCore, libisabelle, examples)
  )

  lazy val pideCore = Project(
    id = "pide-core",
    base = file("pide-core"),
    settings = standardSettings ++ Seq(
      name := "pide-core",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2"
      )
    )
  )

  lazy val libisabelle = Project(
    id = "libisabelle",
    base = file("libisabelle"),
    settings = standardSettings ++ Seq(
      name := "libisabelle",
      libraryDependencies ++= Seq(
        "org.apache.commons" % "commons-compress" % "1.9",
        "org.apache.commons" % "commons-lang3" % "3.3.2",
        "org.specs2" %% "specs2-core" % "3.4" % "test",
        "org.specs2" %% "specs2-scalacheck" % "3.4" % "test",
        "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"
      )
    ),
    dependencies = Seq(pideCore)
  )


  lazy val examples = Project(
    id = "examples",
    base = file("examples"),
    settings = standardSettings ++ Seq(
      name := "examples"
    ),
    dependencies = Seq(libisabelle)
  )

  lazy val full = Project(
    id = "full",
    base = file("full"),
    settings = standardSettings ++ Seq(
      AssemblyKeys.assemblyJarName in AssemblyKeys.assembly := "libisabelle-full.jar"
    ),
    dependencies = Seq(examples)
  )

}
