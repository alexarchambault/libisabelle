package edu.tum.cs.isabelle.setup

import java.io.File
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path, Paths}

import scala.concurrent.{Future, ExecutionContext}

import coursier._
import coursier.{Fetch, MavenRepository}

import org.log4s._

import edu.tum.cs.isabelle.api.{BuildInfo, Environment, Version}

import acyclic.file

/**
 * Detecting and creating [[Setup setups]].
 *
 * This object assumes that there is a ''base path'' in which all Isabelle
 * setups reside. Given a [[edu.tum.cs.isabelle.api.Version version]], the
 * base path can either be searched for an existing setup, or an archive can
 * be downloaded from the Internet and extracted into the path.
 */
object Setup {

  private val logger = getLogger

  /** Default platform: [[Platform.guess guessing]]. */
  def defaultPlatform: Option[Platform] = {
    val guess = Platform.guess
    logger.debug(
      guess match {
        case Some(p) => s"Using default platform; detected $p"
        case None    => "Using default platform; platform could not be detected"
      }
    )
    guess
  }

  val defaultPackageName: String = "edu.tum.cs.isabelle.impl"

  // FIXME this whole thing needs proper error handling

  def install(platform: OfficialPlatform, version: Version)(implicit ec: ExecutionContext): Future[Setup] =
    platform.url(version) match {
      case None =>
        sys.error("couldn't determine URL")
      case Some(url) =>
        logger.debug(s"Downloading setup $version to ${platform.setupStorage}")
        val stream = Tar.download(url)
        Files.createDirectories(platform.setupStorage)
        platform.withLock { () =>
          Tar.extractTo(platform.setupStorage, stream).map(Setup(_, platform, version, defaultPackageName))
        }
    }

  def detectSetup(platform: Platform, version: Version): Option[Setup] = platform.withLock { () =>
    val path = platform.setupStorage(version)
    if (Files.isDirectory(path)) {
      logger.debug(s"Using default setup; detected $version at $path")
      Some(Setup(path, platform, version, defaultPackageName))
    }
    else {
      logger.debug(s"Using default setup; no setup found in ${platform.setupStorage}")
      None
    }
  }

  def defaultSetup(version: Version)(implicit ec: ExecutionContext): Future[Setup] =
    defaultPlatform match {
      case None =>
        sys.error("couldn't determine platform")
      case Some(platform) =>
        detectSetup(platform, version) match {
          case Some(install) =>
            Future.successful(install)
          case None =>
            platform match {
              case o: OfficialPlatform => install(o, version)
              case _ => sys.error("unofficial platform can't be automatically installed")
            }
        }
    }

  def fetchImplementation(platform: Platform, version: Version)(implicit ec: ExecutionContext): Future[List[Path]] = {
    val base = platform.versionedStorage

    val repositories = Seq(
      coursier.Files.ivy2Local,
      MavenRepository("https://repo1.maven.org/maven2/"),
      MavenRepository("https://oss.sonatype.org/content/repositories/releases/")
    )

    val downloadLogger = new coursier.Files.Logger {
      override def downloadingArtifact(url: String) = logger.debug(s"Downloading artifact from $url ...")
      override def downloadedArtifact(url: String, success: Boolean) = {
        val file = url.split('/').last
        if (success)
          logger.debug(s"Successfully downloaded $file")
        else
          logger.error(s"Failed to download $file")
      }
    }

    val files = coursier.Files(
      Seq("https://" -> base.resolve("cache").toFile),
      () => sys.error("impossible")
    )

    val fetch = Fetch(
      repositories,
      files.fetch(logger = Some(downloadLogger))(cachePolicy = CachePolicy.LocalOnly),
      files.fetch(logger = Some(downloadLogger))(cachePolicy = CachePolicy.Default)
    )

    implicit val cachePolicy = CachePolicy.Default

    def resolve(identifier: String) = {
      val dependency =
        Dependency(
          Module(BuildInfo.organization, s"pide-${identifier}_${BuildInfo.scalaBinaryVersion}"),
          BuildInfo.version
        )
      Resolution(Set(dependency)).process.run(fetch).toScalaFuture.map { res =>
        if (!res.isDone)
          sys.error("not converged")
        else if (!res.errors.isEmpty)
          sys.error(s"errors: ${res.errors}")
        else
          res.artifacts.toSet
      }
    }

    platform.withLock { () =>
      for {
        i <- resolve("interface")
        v <- resolve(version.identifier)
        artifacts = v -- i
        res <- Future.traverse(artifacts.toList)(files.file(_, logger = Some(downloadLogger)).run.toScalaFuture)
      }
      yield
        res.map(_.fold(err => sys.error(err.toString), _.toPath))
    }
  }

}

/**
 * A state-less, logic-less representation of a file system location containing
 * an Isabelle installation with a specified
 * [[edu.tum.cs.isabelle.api.Version version]].
 *
 * It is recommended to obtain instances via the [[Setup$ companion object]].
 * No guarantees are made when constructing instances manually.
 *
 * ''Footnote''
 *
 * The file system location is called ''home'' throughout `libisabelle`.
 */
final case class Setup(home: Path, platform: Platform, version: Version, packageName: String) {

  private def instantiate(urls: List[URL]): Environment = {
    val classLoader = new URLClassLoader(urls.toArray, getClass.getClassLoader)
    val env = classLoader.loadClass(s"$packageName.Environment").asSubclass(classOf[Environment])

    val actualVersion = Environment.getVersion(env)
    if (actualVersion != version)
      sys.error(s"expected version $version, got version $actualVersion")

    val info = classLoader.loadClass(s"$packageName.BuildInfo").getDeclaredMethod("toString").invoke(null)
    if (BuildInfo.toString != info.toString)
      sys.error(s"build info does not match")

    val constructor = env.getDeclaredConstructor(classOf[Path])
    constructor.setAccessible(true)
    constructor.newInstance(home)
  }

  /**
   * Convenience method aliasing
   * [[edu.tum.cs.isabelle.Implementations#makeEnvironment]] with the
   * appropriate parameters. It calls [[Setup.fetchImplementation]] to download
   * the required classpath.
   */
  def makeEnvironment(implicit ec: ExecutionContext): Future[Environment] =
    Setup.fetchImplementation(platform, version).map(paths => instantiate(paths.map(_.toUri.toURL)))

}
