package org.apatheia.algorithm.findnode

import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._

import cats.effect.unsafe.implicits.global
import org.mockito.ArgumentMatchers
import org.apatheia.algorithm.findnode.FindNodeClient
import org.apatheia.model.Contact
import org.apatheia.model.NodeId
import org.apatheia.model.RoutingTable
import org.apatheia.network.model.ServerPort
import org.apatheia.network.model.LocalhostMetadata
import org.apatheia.network.meta.LocalhostMetadataRef

class PublisherFindNodeAlgorithmSpec
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar {

  def mockFindNodeClient(expectedContacts: Set[Contact]) =
    new FindNodeClient[IO] {
      override def requestContacts(
          contact: Contact,
          target: NodeId
      ): IO[List[Contact]] =
        IO.pure(expectedContacts.toList)
    }

  val localhostMetadata = LocalhostMetadata(
    localContact = Contact(
      nodeId = NodeId(111),
      port = 1000,
      ip = "0.0.0.0"
    ),
    routingTable = RoutingTable(NodeId(111), Set.empty, 20),
    serverPort = ServerPort(2000),
    responseServerPort = ServerPort(3000)
  )
  val localhostMetadataRef = new LocalhostMetadataRef[IO] {
    override def get: IO[LocalhostMetadata] = IO.pure(localhostMetadata)
    override def modify(localhostMetadata: LocalhostMetadata): IO[Unit] =
      IO.unit
    override def updateRoutingTable(contacts: Set[Contact]): IO[Unit] =
      IO.unit
  }

  behavior of "PublisherFindNodeAlgorithm"

  it should "return the closest contacts list sorted by distance to targetId" in {
    val nodeId1 = NodeId(1)
    val nodeId2 = NodeId(2)
    val nodeId3 = NodeId(3)
    val nodeId4 = NodeId(4)

    val contact1 = Contact(nodeId = nodeId1, ip = "0.0.0.0", port = 8080)
    val contact2 = Contact(nodeId = nodeId2, ip = "0.0.0.0", port = 8080)
    val contact3 = Contact(nodeId = nodeId3, ip = "0.0.0.0", port = 8080)
    val contact4 = Contact(nodeId = nodeId4, ip = "0.0.0.0", port = 8080)

    val routingTable = RoutingTable(
      nodeId = nodeId1,
      contacts = Set(contact2, contact3, contact4)
    )

    val expectedContacts = Set(contact3, contact2, contact4)

    val findNodeAlgorithm = new PublisherFindNodeAlgorithm[IO](
      findNodeClient = mockFindNodeClient(expectedContacts),
      localhostMetadataRef = localhostMetadataRef
    )

    val result = findNodeAlgorithm
      .findNode(routingTable = routingTable, target = nodeId1)
      .unsafeRunSync()

    result shouldBe expectedContacts
  }

  it should "call findClosestContacts on the routing table" in {
    val routingTable = mock[RoutingTable]
    val findNodeClient = mock[FindNodeClient[IO]]
    val findNodeAlgorithm =
      PublisherFindNodeAlgorithm[IO](findNodeClient, localhostMetadataRef, 1)

    when(routingTable.findClosestContacts(NodeId(1)))
      .thenReturn(Set(Contact(NodeId(2), 12345, "localhost")))

    findNodeAlgorithm.findNode(routingTable, NodeId(1))
    verify(routingTable).findClosestContacts(NodeId(1))
  }

  it should "send a find node request for every contact in the closestContacts list" in {
    val routingTable = mock[RoutingTable]
    val findNodeClient = mock[FindNodeClient[IO]]
    val findNodeAlgorithm =
      PublisherFindNodeAlgorithm[IO](findNodeClient, localhostMetadataRef, 1)
    val target = NodeId(1)

    when(routingTable.findClosestContacts(target)).thenReturn(
      Set(
        Contact(NodeId(2), 12345, "localhost"),
        Contact(NodeId(3), 12335, "localhost")
      )
    )

    when(
      findNodeClient.requestContacts(
        Contact(NodeId(2), 12345, "localhost"),
        target
      )
    ).thenReturn(IO(List(Contact(NodeId(4), 12345, "localhost"))))

    when(
      findNodeClient.requestContacts(
        Contact(NodeId(3), 12335, "localhost"),
        target
      )
    ).thenReturn(IO(List(Contact(NodeId(5), 12345, "localhost"))))

    findNodeAlgorithm.findNode(routingTable, NodeId(1)).unsafeRunSync()

    verify(findNodeClient).requestContacts(
      Contact(NodeId(2), 12345, "localhost"),
      target
    )
    verify(findNodeClient).requestContacts(
      Contact(NodeId(3), 12335, "localhost"),
      target
    )
  }

  it should "not go more iterations than necessary" in {
    val mockFindNodeClient = mock[FindNodeClient[IO]]
    val algorithm =
      PublisherFindNodeAlgorithm[IO](
        mockFindNodeClient,
        localhostMetadataRef,
        1
      )
    val routingTable = RoutingTable(nodeId = NodeId(1), contacts = Set.empty)
    val maxIterations = 3
    val targetNodeId = NodeId(2)
    val targetContact = Contact(targetNodeId, 12345, "localhost")

    when(
      mockFindNodeClient
        .requestContacts(
          ArgumentMatchers.any[Contact](),
          ArgumentMatchers.eq[NodeId](targetNodeId)
        )
    ).thenReturn(IO.pure(List.empty[Contact]))

    val result =
      algorithm
        .findNode(target = targetNodeId, routingTable = routingTable)
        .unsafeRunSync()

    result shouldBe Set.empty
  }

}
