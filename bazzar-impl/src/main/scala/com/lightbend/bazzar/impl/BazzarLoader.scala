package com.lightbend.bazzar.impl

import com.lightbend.bazzar.api.BazzarService
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.dns.DnsServiceLocatorComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.server.status.MetricsServiceComponents
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

class BazzarLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new BazzarApplication(context) with DnsServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new BazzarApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[BazzarService])
}

abstract class BazzarApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = LagomServer.forServices(
    bindService[BazzarService].to(wire[BazzarServiceImpl]),
    metricsServiceBinding
  )

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = BazzarSerializerRegistry

  // Register the Bazzar persistent entity
  persistentEntityRegistry.register(wire[BazzarEntity])
}
