package org.apatheia.network.server.impl

import cats.effect.kernel.Async
import cats.implicits._
import org.apatheia.algorithm.findvalue.FindValueAlgorithm
import org.apatheia.codec.Codec._
import org.apatheia.error.FindValueError
import org.apatheia.model.FindValuePayload
import org.apatheia.model.NodeId
import org.apatheia.network.client.UDPClient
import org.apatheia.network.meta.LocalhostMetadataRef
import org.apatheia.network.model.Codecs.FindValueErrorCodec._
import org.apatheia.network.model.Codecs.FindValuePayloadCodec._
import org.apatheia.network.model.Codecs.NodeIdCodec._
import org.apatheia.network.model.KadCommand
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.KadResponseHeaders
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.model.KadResponsePayload
import org.apatheia.network.model.LocalhostMetadata
import org.apatheia.network.model.UDPDatagram
import org.apatheia.network.server.KademliaServerProcessor
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.InetSocketAddress

final class FindValueServerProcessor[F[_]: Async](
    localhostMetadataRef: LocalhostMetadataRef[F],
    findValueSubscriberAlgorithm: FindValueAlgorithm[F],
    requestServerClient: UDPClient[F]
) extends KademliaServerProcessor[F] {

  private val logger = Slf4jLogger.getLogger[F]

  override def process(
      kadDatagram: KadDatagramPackage,
      udpDatagram: UDPDatagram
  ): F[Unit] =
    for {
      localhostMetadataRef <- localhostMetadataRef.get
      response <- findValue(kadDatagram, localhostMetadataRef)
      _ <- sendResponse(
        response,
        udpDatagram,
        kadDatagram,
        localhostMetadataRef
      )
    } yield ()

  private def findValue(
      kadDatagram: KadDatagramPackage,
      localhostMetadata: LocalhostMetadata
  ): F[FindValueAlgorithm.FindValueResult] =
    kadDatagram.payload.data.toObject[NodeId] match {
      case Right(targetId) =>
        findValueSubscriberAlgorithm.findValue(targetId)
      case Left(error) => {
        val errorMessage =
          s"Unexpected error while processing FindValue request for Kademlia Datagram(${kadDatagram.headers})). Error details: ${error.message}"
        logger
          .error(errorMessage)
          .map(_ =>
            Either.left[FindValueError, FindValuePayload](
              FindValueError(errorMessage, Set.empty)
            )
          )
      }
    }

  private def enrichWithMetadata(
      response: FindValueAlgorithm.FindValueResult,
      udpDatagram: UDPDatagram,
      kadDatagram: KadDatagramPackage,
      localhostMetadata: LocalhostMetadata
  ): KadResponsePackage = KadResponsePackage(
    headers = KadResponseHeaders(
      from = localhostMetadata.localContact.nodeId,
      to = kadDatagram.headers.from,
      opId = kadDatagram.headers.opId
    ),
    payload = KadResponsePayload(
      command = KadCommand.FindNode,
      data = response
        .map(_.toByteArray)
        .leftMap(_.toByteArray)
        .toOption
        .getOrElse(Array.empty)
    )
  )

  private def sendResponse(
      response: FindValueAlgorithm.FindValueResult,
      udpDatagram: UDPDatagram,
      kadDatagram: KadDatagramPackage,
      localhostMetadata: LocalhostMetadata
  ): F[Unit] = requestServerClient
    .send(
      targetAddress = new InetSocketAddress(
        udpDatagram.from.getHostName(),
        kadDatagram.headers.responseServerPort.value
      ),
      data = enrichWithMetadata(
        response,
        udpDatagram,
        kadDatagram,
        localhostMetadata
      ).toByteArray
    )
    .map(_ => (): Unit)

}
