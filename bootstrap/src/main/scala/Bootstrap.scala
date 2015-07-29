package edu.tum.cs.isabelle.bootstrap

import java.net.URL
import java.nio.file.Paths

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import edu.tum.cs.isabelle._
import edu.tum.cs.isabelle.api._
import edu.tum.cs.isabelle.setup._

object Bootstrap {

  val implementations = Implementations.empty.addAll(
    BuildInfo.getClass.getDeclaredMethods.filter(_.getName startsWith "Isa").toList.map { m =>
      val urls = m.invoke(BuildInfo).asInstanceOf[Seq[URL]]
      Implementations.Entry(urls.toList, "edu.tum.cs.isabelle.impl.Environment")
    }
  ).get

}

object BootstrapApp extends App {

  val version = Version(args(0))
  println(s"Downloading and untarring $version ...")
  val setup = Await.result(Setup.defaultSetup(version), duration.Duration.Inf)
  println("Loading an environment ...")
  val env = setup.makeEnvironment(Bootstrap.implementations).get
  println("Creating a configuration with default session ...")
  val config = env.Configuration.fromPath(Paths.get("."), s"Protocol${version.identifier}")
  println("Building session ...")
  val built = System.build(env)(config)
  if (built)
    println(s"Success.")
  else
    sys.error("Failed.")

}