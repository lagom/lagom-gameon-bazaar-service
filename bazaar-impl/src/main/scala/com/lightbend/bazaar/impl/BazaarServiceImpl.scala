package com.lightbend.bazaar.impl

import com.lightbend.bazaar.api.BazaarService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

/**
  * Implementation of the BazaarService.
  */
class BazaarServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends BazaarService {
  // Only one singleton instance
  private val Id = "bazaar"

  override def bazaar() = ServiceCall { _ =>
    // Look up the Bazaar entity for the given ID.
    val ref = persistentEntityRegistry.refFor[BazaarEntity](Id)

    // Ask the entity the Bazaar command.
    ref.ask(Bazaar(Id))
  }

  override def useItem() = ServiceCall { request =>
    // Look up the Bazaar entity for the given ID.
    val ref = persistentEntityRegistry.refFor[BazaarEntity](Id)

    // Tell the entity to use the greeting message specified.
    ref.ask(UseItemMessage(request.message))
  }
}
