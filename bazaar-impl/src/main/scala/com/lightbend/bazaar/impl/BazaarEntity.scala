package com.lightbend.bazaar.impl

import java.time.LocalDateTime

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

/**
  * This is an event sourced entity. It has a state, [[BazaarState]], which
  * stores what the item is stored in the Bazaar should be (eg, "Toy").
  *
  * Event sourced entities are interacted with by sending them commands. This
  * entity supports two commands, a [[UseItemMessage]] command, which is
  * used to change the greeting, and a [[Bazaar]] command, which is a read
  * only command which returns a greeting to the name specified by the command.
  *
  * Commands get translated to events, and it's the events that get persisted by
  * the entity. Each event will have an event handler registered for it, and an
  * event handler simply applies an event to the current state. This will be done
  * when the event is first created, and it will also be done when the entity is
  * loaded from the database - each event will be replayed to recreate the state
  * of the entity.
  *
  * This entity defines one event, the [[ItemMessageChanged]] event,
  * which is emitted when a [[UseItemMessage]] command is received.
  */
class BazaarEntity extends PersistentEntity {

  override type Command = BazaarCommand[_]
  override type Event = BazaarEvent
  override type State = BazaarState

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: BazaarState = BazaarState("Pen", LocalDateTime.now.toString)

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case BazaarState(message, _) => Actions().onCommand[UseItemMessage, Done] {

      // Command handler for the UseItemMessage command
      case (UseItemMessage(newMessage), ctx, state) =>
        // In response to this command, we want to first persist it as a
        // ItemMessageChanged event
        ctx.thenPersist(
          ItemMessageChanged(newMessage)
        ) { _ =>
          // Then once the event is successfully persisted, we respond with done.
          ctx.reply(Done)
        }

    }.onReadOnlyCommand[Bazaar, String] {

      // Command handler for the Bazaar command
      case (Bazaar(name), ctx, state) =>
        // Reply with a message built from the current message, and the name of
        // the name of the bazaar item we are putting in.
        ctx.reply(message)

    }.onEvent {

      // Event handler for the ItemMessageChanged event
      case (ItemMessageChanged(newMessage), state) =>
        // We simply update the current state to use the greeting message from
        // the event.
        BazaarState(newMessage, LocalDateTime.now().toString)

    }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class BazaarState(message: String, timestamp: String)

object BazaarState {
  /**
    * Format for the bazaar state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the entity gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[BazaarState] = Json.format
}

/**
  * This interface defines all the events that the BazaarEntity supports.
  */
sealed trait BazaarEvent extends AggregateEvent[BazaarEvent] {
  def aggregateTag = BazaarEvent.Tag
}

object BazaarEvent {
  val Tag = AggregateEventTag[BazaarEvent]
}

/**
  * An event that represents a change in greeting message.
  */
case class ItemMessageChanged(message: String) extends BazaarEvent

object ItemMessageChanged {

  /**
    * Format for the greeting message changed event.
    *
    * Events get stored and loaded from the database, hence a JSON format
    * needs to be declared so that they can be serialized and deserialized.
    */
  implicit val format: Format[ItemMessageChanged] = Json.format
}

/**
  * This interface defines all the commands that the Bazaar entity supports.
  */
sealed trait BazaarCommand[R] extends ReplyType[R]

/**
  * A command to switch the greeting message.
  *
  * It has a reply type of [[Done]], which is sent back to the caller
  * when all the events emitted by this command are successfully persisted.
  */
case class UseItemMessage(message: String) extends BazaarCommand[Done]

object UseItemMessage {

  /**
    * Format for the use greeting message command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[UseItemMessage] = Json.format
}

/**
  * A command to say bazaar to someone using the current greeting message.
  *
  * The reply type is String, and will contain the message to say to that
  * person.
  */
case class Bazaar(name: String) extends BazaarCommand[String]

object Bazaar {

  /**
    * Format for the bazaar command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[Bazaar] = Json.format
}

/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object BazaarSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[UseItemMessage],
    JsonSerializer[Bazaar],
    JsonSerializer[ItemMessageChanged],
    JsonSerializer[BazaarState]
  )
}
