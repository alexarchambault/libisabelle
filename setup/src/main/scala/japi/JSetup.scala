package edu.tum.cs.isabelle.japi

import java.nio.file.Path

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import edu.tum.cs.isabelle.api._
import edu.tum.cs.isabelle.setup._

object JSetup {

  def defaultSetup(version: Version, timeout: Duration): Setup =
    Await.result(Setup.defaultSetup(version), timeout)

  def defaultSetup(version: Version): Setup =
    defaultSetup(version, Duration.Inf)

  def makeEnvironment(setup: Setup, timeout: Duration): Environment =
    Await.result(setup.makeEnvironment, timeout)

  def makeEnvironment(setup: Setup): Environment =
    makeEnvironment(setup, Duration.Inf)

}
