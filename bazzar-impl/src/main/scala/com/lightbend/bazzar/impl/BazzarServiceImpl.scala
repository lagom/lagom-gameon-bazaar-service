package com.lightbend.bazzar.impl

import com.lightbend.bazzar.api.BazzarService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

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
}
