package com.lightbend.bazaar.impl

import com.lightbend.bazaar.api.BazaarService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

/**
  * Implementation of the BazaarService.
  */
class BazaarServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends BazaarService {

  override def bazaar(id: String) = ServiceCall { _ =>
    // Look up the Bazaar entity for the given ID.
    val ref = persistentEntityRegistry.refFor[BazaarEntity](id)

    // Ask the entity the Bazaar command.
    ref.ask(Bazaar(id))
  }

  override def useItem(id: String) = ServiceCall { request =>
    // Look up the Bazaar entity for the given ID.
    val ref = persistentEntityRegistry.refFor[BazaarEntity](id)

    // Tell the entity to use the greeting message specified.
    ref.ask(UseItemMessage(request.message))
  }
}
