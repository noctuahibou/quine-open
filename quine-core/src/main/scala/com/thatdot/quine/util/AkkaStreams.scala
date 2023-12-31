package com.thatdot.quine.util

import scala.concurrent.Future

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink}

object AkkaStreams {

  def count: Sink[Any, Future[Int]] = Sink.fold[Int, Any](0)((n, _) => n + 1)

  /** Remove consecutive duplicates
    */
  def distinct[A >: Null]: Flow[A, A, NotUsed] = Flow[A].statefulMapConcat { () =>
    var previousElement: A = null

    { element =>
      val duplicate = element == previousElement
      if (duplicate) {
        Nil
      } else {
        previousElement = element
        element :: Nil
      }
    }
  }

}
