package edu.tum.cs.isabelle

import scala.concurrent.duration._

import isabelle.XML

import org.specs2.Specification
import org.specs2.matcher.Matcher
import org.specs2.time.NoTimeConversions

class LibisabelleSpec extends Specification with NoTimeConversions { def is = s2"""

  Basic protocol interaction

  An Isabelle session
    can be started          $start
    can load theories       $load
    reacts to requests      $req
    can be torn down        $stop"""


  val TypeOf = Operation.implicitly[String, String]("type_of")

  val system = System.instance(Some(new java.io.File(".")), "Protocol")
  val loaded = system.flatMap(_.loadTheories(new java.io.File("libisabelle/src/test/isabelle/Test")))
  val response = for { s <- system; _ <- loaded; res <- s.invoke(TypeOf)("op ==>") } yield res
  val teardown = for { s <- system; _ <- response /* wait for response */; _ <- s.dispose } yield ()

  def exist[A]: Matcher[A] = ((a: A) => a != null, "doesn't exist")


  def start = system must exist.await(timeout = 30.seconds)
  def load = loaded must beTrue.await(timeout = 30.seconds)
  def req = response must beRight("prop => prop => prop").await(timeout = 30.seconds)
  def stop = teardown must exist.await(timeout = 30.seconds)

}