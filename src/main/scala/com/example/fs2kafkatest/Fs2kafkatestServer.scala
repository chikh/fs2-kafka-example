package com.example.fs2kafkatest

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global
import fs2.kafka._

object Fs2kafkatestServer {

  def stream[F[_]: ConcurrentEffect: ContextShift](implicit
      T: Timer[F] /*, C: ContextShift[F]*/
  ): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global).stream
      producerSettings =
        ProducerSettings[F, String, String]
          .withBootstrapServers("localhost:9092")
      producer <- producerStream[F].using(producerSettings)
      helloWorldAlg = HelloWorld.impl[F]
      kafkaAlg = KafkaSender.impl[F](producer)
      jokeAlg = Jokes.impl[F](client)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
          Fs2kafkatestRoutes.helloWorldRoutes[F](helloWorldAlg, kafkaAlg) <+>
            Fs2kafkatestRoutes.jokeRoutes[F](jokeAlg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(8888, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
