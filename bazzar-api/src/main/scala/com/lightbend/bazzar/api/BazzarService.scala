package com.lightbend.bazzar.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}

object BazzarService  {
  val TOPIC_NAME = "items"
}

/**
  * The Bazzar service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the BazzarService.
  */
trait BazzarService extends Service {

  /**
    * Example: curl http://localhost:9000/api/bazzar/Toy
    */
  def bazzar(id: String): ServiceCall[NotUsed, String]

  /**
    * Example: curl -H "Content-Type: application/json" -X POST -d '{"message": "Bazzar"}' http://localhost:9000/api/bazzar/Toy
    */
  def useItem(id: String): ServiceCall[ItemMessage, Done]

  /**
    * This gets published to Kafka.
    */
  def itemsTopic(): Topic[ItemMessageChanged]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("bazzar")
      .withCalls(
        pathCall("/api/bazzar/:id", bazzar _),
        pathCall("/api/bazzar/:id", useItem _)
      )
      .withTopics(
        topic(BazzarService.TOPIC_NAME, itemsTopic _)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[ItemMessageChanged](_.name)
          )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

/**
  * The item message class.
  */
case class ItemMessage(message: String)

object ItemMessage {
  /**
    * Format for converting item messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[ItemMessage] = Json.format[ItemMessage]
}

/**
  * The item message class used by the topic stream.
  * Different than [[ItemMessage]], this message includes the name (id).
  */
case class ItemMessageChanged(name: String, message: String)

object ItemMessageChanged {
  /**
    * Format for converting item messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[ItemMessageChanged] = Json.format[ItemMessageChanged]
}
