package edu.tum.cs.isabelle.tests

import scala.concurrent.duration._

import org.specs2.Specification
import org.specs2.specification.core.Env
import org.specs2.matcher.Matcher

import edu.tum.cs.isabelle._
import edu.tum.cs.isabelle.api._

class LibisabelleSpec(val specs2Env: Env) extends Specification with DefaultSetup { def is = s2"""

  Basic protocol interaction

  An Isabelle session
    can be started          ${system must exist.awaitFor(30.seconds)}
    can load theories       ${loaded must beRight(()).awaitFor(30.seconds)}
    reacts to requests      ${response must beRight("prop => prop => prop").awaitFor(30.seconds)}
    can be torn down        ${teardown must exist.awaitFor(30.seconds)}"""


  implicit val ee = specs2Env.executionEnv

  val TypeOf = Operation.implicitly[String, String]("type_of")

  val system = System.create(env)(config)
  val loaded = system.flatMap(_.invoke(Operation.UseThys)(List("tests/src/test/isabelle/Test")))
  val response = for { s <- system; _ <- loaded; res <- s.invoke(TypeOf)("op ==>") } yield res
  val teardown = for { s <- system; _ <- response /* wait for response */; _ <- s.dispose } yield ()

  def exist[A]: Matcher[A] = ((a: A) => a != null, "doesn't exist")

}