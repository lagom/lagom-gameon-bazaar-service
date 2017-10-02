import com.typesafe.sbt.packager.docker.{ Cmd, ExecCmd }
import sbt.Resolver.bintrayRepo

organization in ThisBuild := "com.lightbend"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.11.8"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test

lazy val `bazzar` = (project in file("."))
  .aggregate(`bazzar-api`, `bazzar-impl`)

lazy val `bazzar-api` = (project in file("bazzar-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `bazzar-impl` = (project in file("bazzar-impl"))
  .enablePlugins(LagomScala)
  .settings(
    dockerRepository := Some("registry.ng.bluemix.net/javaone"),
    dockerUpdateLatest := true,
    dockerEntrypoint ++= """-Dplay.crypto.secret="${APPLICATION_SECRET:-none}" -Dplay.akka.actor-system="${AKKA_ACTOR_SYSTEM_NAME:-bazzar-v1}" -Dhttp.address="$GAMEONSERVICE_BIND_IP" -Dhttp.port="$GAMEONSERVICE_BIND_PORT" -Dakka.actor.provider=cluster -Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")" -Dakka.remote.netty.tcp.port="$AKKA_REMOTING_BIND_PORT" $(IFS=','; I=0; for NODE in $AKKA_SEED_NODES; do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://$AKKA_ACTOR_SYSTEM_NAME@$NODE"; I=$(expr $I + 1); done) -Dakka.io.dns.resolver=async-dns -Dakka.io.dns.async-dns.resolve-srv=true -Dakka.io.dns.async-dns.resolv-conf=on""".split(" ").toSeq,
    dockerCommands := dockerCommands.value.flatMap {
      case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
      case v => Seq(v)
    },
    resolvers += bintrayRepo("hajile", "maven"),
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      "com.lightbend" %% "lagom13-scala-service-locator-dns" % "2.0.0",
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`bazzar-api`)
