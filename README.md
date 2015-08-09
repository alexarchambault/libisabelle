# libisabelle
Minimal wrapper around Isabelle/PIDE for non-IDE applications

[![Build Status](https://travis-ci.org/larsrh/libisabelle.svg?branch=master)](https://travis-ci.org/larsrh/libisabelle)

## Setup

`libisabelle` is a Scala library which talks to Isabelle.
It currently works with Isabelle2014 and Isabelle2015, but Linux-only.

To get started, follow these steps:

1. Run the `sbt` script to fetch all required Scala dependencies.
   After this is done, you are in the SBT shell.
2. Type `bootstrap/run 2015`, which will download and extract the latest supported Isabelle version for you.

On some systems, you might need to install Perl, Python, and/or some additional libraries.

Note to proficient Isabelle users:
`libisabelle` does not respect `ISABELLE_HOME` by default.
Bootstrapping will create a new installation in the `contrib` folder.

## Documentation

You can browse the Scaladoc [directly at Sonatype](https://oss.sonatype.org/service/local/repositories/snapshots/archive/info/hupel/libisabelle-docs_2.10/0.1-SNAPSHOT/libisabelle-docs_2.10-0.1-SNAPSHOT-javadoc.jar/!/index.html).

## Running the tests

Run the `sbt` script again, then, in the SBT shell, type `test`.
This requires the environment variable `ISABELLE_VERSION` to be set.
Another option is to pass the version to the test framework directly.

Example:

```
$ cd libisabelle
$ ./sbt
...
> testOnly * -- isabelle.version 2015
```

Make sure to have bootstrapped the installation as described above for the appropriate Isabelle version, otherwise the tests will fail.

## Including libisabelle into your project

`libisabelle` is cross-built for Scala 2.10.x, 2.11.x and 2.12.x.
Drop the following lines into your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "info.hupel" %% "libisabelle" % "0.1-SNAPSHOT",
  "info.hupel" %% "libisabelle-setup" % "0.1-SNAPSHOT",
  "info.hupel" %% "pide-interface" % "0.1-SNAPSHOT"
)
```

Depending on which Isabelle version you want, also add either of those:

```scala
libraryDependencies += "info.hupel" %% "pide-2014" % "0.1-SNAPSHOT"
libraryDependencies += "info.hupel" %% "pide-2015" % "0.1-SNAPSHOT"
```
