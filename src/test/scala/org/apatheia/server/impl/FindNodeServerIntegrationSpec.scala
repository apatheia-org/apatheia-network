package org.apatheia.server.impl

import org.scalatest.flatspec.AnyFlatSpec
import org.apatheia.network.server.impl.DefaultKademliaDatagramServerReceiver
import org.apatheia.model.Contact
import org.apatheia.network.model.LocalhostMetadata
import org.apatheia.model.NodeId
import org.apatheia.model.RoutingTable
import cats.effect.std.AtomicCell
import cats.effect.IO
import org.apatheia.network.server.impl.DefaultUDPServer
import org.apatheia.network.model.ServerPort
import org.apatheia.network.meta.DefaultLocalhostMetadataRef
import org.apatheia.store.KeyValueStore
import org.apatheia.store.ApatheiaKeyValueStore
import org.apatheia.network.model.OpId
import org.apatheia.network.model.KadResponsePackage
import scala.collection.immutable.HashMap
import org.apatheia.network.client.response.store.DefaultResponseStoreRef
import org.apatheia.network.client.response.KadResponseDatagramReceiver
import cats.effect.std.Dispatcher
import org.apatheia.network.model.MaxClientBufferSize
import org.apatheia.network.client.impl.DefaultUDPClient
import org.apatheia.network.model.MaxClientTimeout
import org.apatheia.network.client.response.consumer.DefaultKadResponseConsumer
import org.apatheia.algorithm.findnode.FindNodeClient
import org.apatheia.algorithm.findnode.DefaultFindNodeClient
import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.kernel.Async
import java.net.InetAddress
import java.net.Inet4Address
import java.net.InetSocketAddress
import org.scalatest.matchers.should.Matchers

class FindNodeIntegrationSpec extends AnyFlatSpec with Matchers {

  private val logger = Slf4jLogger.getLogger[IO]

  "DefaultKademliaDatagramServerReceiver" should "process kademlia requests from a FindNodeClient" in {
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

    val localhostMetadata = LocalhostMetadata(
      localContact = localContact,
      routingTable = RoutingTable(localNodeId, List(localContact)),
      serverPort = localServerPort,
      responseServerPort = responseServerPort
    )

    val remoteMetadata = LocalhostMetadata(
      localContact = targetContact,
      routingTable = RoutingTable(remoteNodeId, List(targetContact)),
      serverPort = remoteServerPort,
      responseServerPort = remoteResponseServerPort
    )

    Dispatcher[IO]
      .use(implicit dispatcher => {
        for {
          cell <- AtomicCell[IO].of[LocalhostMetadata](
            localhostMetadata
          )
          localhostMetadataRef = DefaultLocalhostMetadataRef[IO](cell)
          remoteCell <- AtomicCell[IO].of[LocalhostMetadata](
            remoteMetadata
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
            requestServerClient = responseServerClient
          )
          kademliaUdpServer = DefaultUDPServer[IO](
            serverPort = remoteServerPort,
            receiver = kademliaServerProcessor
          )
          kademliaResponseConsumer = DefaultKadResponseConsumer[IO](
            responseKeyStore = defaultResponseStoreRef
          )
          findNodeClient = DefaultFindNodeClient[IO](
            kadResponseConsumer = kademliaResponseConsumer,
            responseTimeout = 10.seconds,
            udpClient = requestServerClient,
            defaultLocalhostMetadataRef = localhostMetadataRef
          )
          // create server fibers
          responseServerFiber = responseServer.run().start
          kademliaUdpServerFiber = kademliaUdpServer.run().start

          // potential disruption to run the server. it should run in parallel
          _ <- responseServerFiber
          _ <- IO(Thread.sleep(1000)) // Wait for the server to start up
          _ <- kademliaUdpServerFiber
          _ <- IO(Thread.sleep(1000)) // Wait for the server to start up

          // if execution continues the response should contain the contacts
          _ <- logger.info(
            s"Sending request to ${targetContact}/$remoteNodeId)"
          )
          contacts <- findNodeClient
            .requestContacts(
              remote = targetContact,
              target = remoteNodeId
            )
          _ <- logger.info(
            s"Received the remote contact as response: ${contacts}"
          )
        } yield (contacts)
      })
      .unsafeRunSync() should not be empty
  }

}
