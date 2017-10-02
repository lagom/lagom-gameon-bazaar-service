package com.lightbend.bazaar.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.{ Service, ServiceCall }
import play.api.libs.json.{ Format, Json }

object BazaarService  {
  val TOPIC_NAME = "items"
}

/**
  * The Bazaar service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the BazaarService.
  */
trait BazaarService extends Service {

  /**
    * Returns the item currently in the Bazaar.
    * Example: curl http://localhost:9000/api/bazaar
    */
  def bazaar(): ServiceCall[NotUsed, String]

  /**
    * Changes the item currently in the Bazaar.
    * Example: curl -H "Content-Type: application/json" -X POST -d '{"message": "Toy"}' http://localhost:9000/api/bazaar
    */
  def useItem(): ServiceCall[ItemMessage, Done]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("bazaar")
      .withCalls(
        pathCall("/api/bazaar", bazaar _),
        pathCall("/api/bazaar", useItem _)
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
