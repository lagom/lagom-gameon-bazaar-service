package com.lightbend.bazaar.impl

import com.lightbend.bazaar.api.BazaarService
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.dns.DnsServiceLocatorComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.server.status.MetricsServiceComponents
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

class BazaarLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new BazaarApplication(context) with DnsServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new BazaarApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[BazaarService])
}

abstract class BazaarApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = LagomServer.forServices(
    bindService[BazaarService].to(wire[BazaarServiceImpl]),
    metricsServiceBinding
  )

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = BazaarSerializerRegistry

  // Register the Bazaar persistent entity
  persistentEntityRegistry.register(wire[BazaarEntity])
}
