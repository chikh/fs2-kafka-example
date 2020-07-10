package com.example.fs2kafkatest

import cats._
import cats.implicits._
import fs2.kafka._

trait KafkaSender[F[_]] {
  def send(event: String): F[ProducerResult[String, String, Unit]]
}

object KafkaSender {
  def impl[F[_]: FlatMap](producer: KafkaProducer[F, String, String]) = {

    new KafkaSender[F] {
      override def send(event: String) =
        producer
          .produce(
            ProducerRecords.one(
              ProducerRecord("hellos-2", "test-key", event)
            )
          )
          .flatten
    }
  }
}
