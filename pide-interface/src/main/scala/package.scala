package edu.tum.cs.isabelle.api

object `package` {

  type Properties = List[(String, String)]
  type Markup = (String, Properties)

  type XMLBody = List[XMLTree]

  type ProverResult[+T] = Either[Throwable, T]

}