package org.apatheia.network.server.impl

import cats.effect.kernel.Async
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.server.KademliaServerProcessor
import org.apatheia.network.meta.LocalhostMetadataRef
import org.apatheia.model.NodeId
import org.apatheia.algorithm.findnode.sub.FindNodeSubscriberAlgorithm
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.apatheia.network.client.UDPClient
import org.apatheia.model.Contact
import org.apatheia.network.model.LocalhostMetadata
import org.apatheia.network.model.KadResponseHeaders
import org.apatheia.network.model.UDPDatagram
import java.net.InetSocketAddress
import cats.implicits._
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.model.KadCommand
import org.apatheia.network.model.KadResponsePayload
import org.apatheia.network.model.Tag

case class FindNodeServerProcessor[F[_]: Async](
    localhostMetadataRef: LocalhostMetadataRef[F],
    findNodeSubscriberAlgorithm: FindNodeSubscriberAlgorithm[F],
    requestServerClient: UDPClient[F]
) extends KademliaServerProcessor[F] {

  private val logger = Slf4jLogger.getLogger[F]

  override def process(
      kadDatagram: KadDatagramPackage,
      udpDatagram: UDPDatagram
  ): F[Unit] = for {
    localhostMetadataRef <- localhostMetadataRef.get
    contacts <- findNode(
      kadDatagram,
      localhostMetadataRef
    )
    _ <- sendResponse(
      contacts,
      udpDatagram,
      kadDatagram,
      localhostMetadataRef
    )
  } yield ()

  private def findNode(
      udpDatagram: KadDatagramPackage,
      localhostMetadata: LocalhostMetadata
  ): F[List[Contact]] = NodeId.parse(udpDatagram.payload.data) match {
    case Right(targetId) =>
      findNodeSubscriberAlgorithm.findNode(
        targetId,
        localhostMetadata.routingTable
      )
    case Left(error) =>
      logger
        .error(
          s"Unexpected error while processing FindNode request for Kademlia Datagram(${udpDatagram.headers}))"
        )
        .map(_ => List.empty)
  }

  private def formatWithTag(contact: Contact): Array[Byte] =
    Array.concat(Tag.Contact.tagData, contact.toByteArray)

  private def enrichWithMetadata(
      contactsData: Array[Byte],
      kadDatagram: KadDatagramPackage,
      localhostMetadata: LocalhostMetadata,
      udpDatagram: UDPDatagram
  ): KadResponsePackage = KadResponsePackage(
    headers = KadResponseHeaders(
      from = localhostMetadata.localContact.nodeId,
      to = kadDatagram.headers.from,
      opId = kadDatagram.headers.opId
    ),
    payload = KadResponsePayload(command = KadCommand.FindNode, contactsData)
  )

  private def sendResponse(
      contacts: List[Contact],
      udpDatagram: UDPDatagram,
      kadDatagram: KadDatagramPackage,
      localhostMetadata: LocalhostMetadata
  ): F[Unit] = for {
    contactsData <- Async[F].pure {
      contacts
        .map(formatWithTag)
        .toArray
        .flatten
    }
    kadResponsePackage = enrichWithMetadata(
      contactsData,
      kadDatagram,
      localhostMetadata,
      udpDatagram
    )
    result <- requestServerClient.send(
      targetAddress = new InetSocketAddress(
        udpDatagram.from.getHostName(),
        kadDatagram.headers.responseServerPort.value
      ),
      data = kadResponsePackage.toByteArray
    )
  } yield ()

}
