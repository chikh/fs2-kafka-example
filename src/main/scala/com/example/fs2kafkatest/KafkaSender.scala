package com.example.fs2kafkatest

import cats.effect._
import cats.implicits._
import cats._
import fs2.kafka._

trait KafkaSender[F[_]] {
  def send(event: String): F[ProducerResult[String, String, Unit]]
}

object KafkaSender {
  def impl[F[_]: ConcurrentEffect: ContextShift: FlatMap] = {
    val producerSettings =
      ProducerSettings[F, String, String]
        .withBootstrapServers("localhost:9092")

    val resource = producerResource[F].using(producerSettings)

    new KafkaSender[F] {
      override def send(event: String) =
        resource
          .use(producer =>
            producer
              .produce(
                ProducerRecords.one(
                  ProducerRecord("jokes", "test-key", event)
                )
              )
              .flatten
          )
    }
  }
}
