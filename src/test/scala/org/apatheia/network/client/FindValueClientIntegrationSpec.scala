package org.apatheia.network.client

import cats.effect.IO
import cats.effect.std.AtomicCell
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import org.apatheia.model.Contact
import org.apatheia.model.NodeId
import org.apatheia.model.RoutingTable
import org.apatheia.network.client.impl.DefaultUDPClient
import org.apatheia.network.client.response.KadResponseDatagramReceiver
import org.apatheia.network.client.response.consumer.DefaultKadResponseConsumer
import org.apatheia.network.client.response.store.DefaultResponseStoreRef
import org.apatheia.network.meta.DefaultLocalhostMetadataRef
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.model.LocalhostMetadata
import org.apatheia.network.model.MaxClientBufferSize
import org.apatheia.network.model.MaxClientTimeout
import org.apatheia.network.model.OpId
import org.apatheia.network.model.ServerPort
import org.apatheia.network.server.impl.DefaultKademliaDatagramServerReceiver
import org.apatheia.network.server.impl.DefaultUDPServer
import org.apatheia.store.ApatheiaKeyValueStore
import org.apatheia.store.KeyValueStore
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.InetSocketAddress
import scala.collection.immutable.HashMap
import scala.concurrent.duration._

class FindValueClientIntegrationSpec
    extends AnyFlatSpec
    with Matchers
    with EitherValues {
  private val logger = Slf4jLogger.getLogger[IO]

  "DefaultKademliaDatagramServerReceiver" should "process kademlia requests from a FindValueClient" in new TestContext {
    val findValueResult = Dispatcher[IO]
      .use(implicit dispatcher => {
        for {
          cell <- AtomicCell[IO].of[LocalhostMetadata](
            localhostMetadata
          )
          apatheiaKeyValueStore = ApatheiaKeyValueStore[NodeId, Array[Byte]](
            HashMap(remoteNodeId -> Array[Byte](0, 1, 0, 1))
          )
          localhostMetadataRef = DefaultLocalhostMetadataRef[IO](cell)
          remoteCell <- AtomicCell[IO].of[LocalhostMetadata](
            remoteMetadataNoTarget
          )
          remoteLocalhostMetadataRef = DefaultLocalhostMetadataRef[IO](
            remoteCell
          )
          keyValueStoreCell <- AtomicCell[IO]
            .of[KeyValueStore[OpId, KadResponsePackage]](
              ApatheiaKeyValueStore[OpId, KadResponsePackage](
                HashMap.empty
              )
            )
          defaultResponseStoreRef = DefaultResponseStoreRef[IO](
            keyValueStoreCell
          )
          kademliaResponseReceiver = KadResponseDatagramReceiver[IO](
            defaultResponseStoreRef
          )
          responseServer = DefaultUDPServer[IO](
            serverPort = responseServerPort,
            receiver = kademliaResponseReceiver
          )
          requestServerClient = DefaultUDPClient[IO](
            MaxClientBufferSize(1024 * 10),
            MaxClientTimeout(10)
          )
          responseServerClient = DefaultUDPClient[IO](
            MaxClientBufferSize(1024 * 10),
            MaxClientTimeout(10)
          )
          kademliaServerProcessor = DefaultKademliaDatagramServerReceiver[IO](
            localhostMetadataRef = remoteLocalhostMetadataRef,
            requestServerClient = responseServerClient,
            apatheiaKeyValueStore = apatheiaKeyValueStore
          )
          kademliaUdpServer = DefaultUDPServer[IO](
            serverPort = remoteServerPort,
            receiver = kademliaServerProcessor
          )
          kademliaResponseConsumer = DefaultKadResponseConsumer[IO](
            responseKeyStore = defaultResponseStoreRef
          )
          findValueClient = new DefaultFindValueClient[IO](
            kadResponseConsumer = kademliaResponseConsumer,
            responseTimeout = 10.seconds,
            udpClient = requestServerClient,
            localhostMetadataRef = localhostMetadataRef
          )
          // create server fibers
          responseServerFiber = responseServer.run().start
          kademliaUdpServerFiber = kademliaUdpServer.run().start

          // potential disruption to run the server. it should run in parallel
          _ <- responseServerFiber
          _ <- IO.sleep(1.second) // Wait for the server to start up
          _ <- kademliaUdpServerFiber
          _ <- IO.sleep(1.second) // Wait for the server to start up

          // if execution continues the response should contain the contacts
          _ <- logger.info(
            s"Sending request to ${targetContact}/$remoteNodeId)"
          )
          // get data throught kademlia FIND_VALUE operation
          remoteData <- findValueClient
            .sendFindValue(remoteNodeId, targetContact)
          _ <- logger.info(
            s"Received remote data as response: ${remoteData}"
          )
        } yield (remoteData)
      })
      .unsafeRunSync()

    findValueResult.map(_.contact) shouldBe Right(targetContact)
  }

  it should "receive all contacts pointing to target node id when that target node is not found" in new TestContext {}

  it should "breach timeout if no response is received" in new TestContext {}

  trait TestContext {
    val localNodeId = NodeId(1)
    val remoteNodeId = NodeId(2)
    val inetSocketAddress = new InetSocketAddress(3333)
    val responseServerPort = ServerPort(3333)
    val localServerPort = ServerPort(4444)

    val remoteResponseServerPort = ServerPort(5555)
    val remoteServerPort = ServerPort(6666)

    val localContact = Contact(
      nodeId = localNodeId,
      port = localServerPort.value,
      ip = "127.0.0.1"
    )
    val targetContact = Contact(
      nodeId = remoteNodeId,
      port = remoteServerPort.value,
      ip = "127.0.0.1"
    )
    val nonTargetContact1 = Contact(
      nodeId = NodeId(20000),
      port = 20000,
      ip = "127.0.0.1"
    )
    val nonTargetContact2 = Contact(
      nodeId = NodeId(30000),
      port = 30000,
      ip = "127.0.0.1"
    )

    val localhostMetadata = LocalhostMetadata(
      localContact = localContact,
      routingTable = RoutingTable(localNodeId, Set(localContact)),
      serverPort = localServerPort,
      responseServerPort = responseServerPort
    )

    val remoteMetadata = LocalhostMetadata(
      localContact = targetContact,
      routingTable = RoutingTable(remoteNodeId, Set(targetContact)),
      serverPort = remoteServerPort,
      responseServerPort = remoteResponseServerPort
    )

    val remoteMetadataNoTarget = LocalhostMetadata(
      localContact = targetContact,
      routingTable =
        RoutingTable(remoteNodeId, Set(nonTargetContact1, nonTargetContact2)),
      serverPort = remoteServerPort,
      responseServerPort = remoteResponseServerPort
    )

  }

}
