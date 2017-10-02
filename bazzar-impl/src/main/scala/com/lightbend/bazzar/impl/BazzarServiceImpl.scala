package com.lightbend.bazzar.impl

import com.lightbend.bazzar.api
import com.lightbend.bazzar.api.{BazzarService}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}

/**
  * Implementation of the BazzarService.
  */
class BazzarServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends BazzarService {

  override def bazzar(id: String) = ServiceCall { _ =>
    // Look up the Bazzar entity for the given ID.
    val ref = persistentEntityRegistry.refFor[BazzarEntity](id)

    // Ask the entity the Bazzar command.
    ref.ask(Bazzar(id))
  }

  override def useItem(id: String) = ServiceCall { request =>
    // Look up the Bazzar entity for the given ID.
    val ref = persistentEntityRegistry.refFor[BazzarEntity](id)

    // Tell the entity to use the greeting message specified.
    ref.ask(UseItemMessage(request.message))
  }


  override def itemsTopic(): Topic[api.ItemMessageChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(BazzarEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(bazzarEvent: EventStreamElement[BazzarEvent]): api.ItemMessageChanged = {
    bazzarEvent.event match {
      case ItemMessageChanged(msg) => api.ItemMessageChanged(bazzarEvent.entityId, msg)
    }
  }
}
