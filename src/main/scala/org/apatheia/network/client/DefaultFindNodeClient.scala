package org.apatheia.algorithm.findnode

import cats.effect.kernel.Sync
import cats.implicits._
import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure
import org.apatheia.model.Contact
import org.apatheia.model.NodeId
import org.apatheia.network.client.UDPClient
import org.apatheia.network.client.response.consumer.KadResponseConsumer
import org.apatheia.network.meta.LocalhostMetadataRef
import org.apatheia.network.model.Codecs.ContactSetCodec._
import org.apatheia.network.model.Codecs.NodeIdCodec._
import org.apatheia.network.model.KadCommand
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.KadDatagramPayload
import org.apatheia.network.model.KadRequestHeaders
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.model.OpId
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.InetSocketAddress
import scala.concurrent.duration.Duration

final case class DefaultFindNodeClient[F[_]: Sync](
    kadResponseConsumer: KadResponseConsumer[F],
    responseTimeout: Duration,
    udpClient: UDPClient[F],
    defaultLocalhostMetadataRef: LocalhostMetadataRef[F]
) extends FindNodeClient[F] {

  private val logger = Slf4jLogger.getLogger[F]

  override def requestContacts(
      remote: Contact,
      target: NodeId
  ): F[List[Contact]] = {
    findContacts(remote, target).flatMap {
      case Left(error) =>
        logger.error(error.message) *> Sync[F].pure(List.empty)
      case Right(contacts) => Sync[F].pure(contacts)
    }
  }

  private def findContacts(
      remote: Contact,
      target: NodeId
  ): F[Either[DecodingFailure, List[Contact]]] = for {
    kadDatagramPackage <- toKadDatagramPackage(remote, target)
    _ <- udpClient.send(
      new InetSocketAddress(remote.ip, remote.port),
      kadDatagramPackage.toByteArray
    )
    response <- kadResponseConsumer.consumeResponse(
      opId = kadDatagramPackage.headers.opId,
      timeout = responseTimeout
    )
    contacts <- toContacts(response)
  } yield (contacts)

  private def toKadDatagramPackage(
      remote: Contact,
      target: NodeId
  ): F[KadDatagramPackage] =
    defaultLocalhostMetadataRef.get.map(locahostMetadata =>
      KadDatagramPackage(
        headers = KadRequestHeaders(
          from = locahostMetadata.localContact.nodeId,
          to = remote.nodeId,
          opId = OpId.random,
          responseServerPort = locahostMetadata.responseServerPort
        ),
        payload = KadDatagramPayload(
          command = KadCommand.FindNode,
          data = target.toByteArray
        )
      )
    )

  private def toContacts(
      responsePackage: Option[KadResponsePackage]
  ): F[Either[DecodingFailure, List[Contact]]] =
    responsePackage
      .map(responsePkg =>
        if (responsePkg.payload.command != KadCommand.FindNode) {
          raiseUnexpectedCommandError(responsePkg.headers.opId)
        } else {
          parsePayloadDataToContacts(responsePkg)
        }
      )
      .getOrElse(Sync[F].pure(Right(List.empty)))

  private def raiseUnexpectedCommandError(
      opId: OpId
  ): F[Either[DecodingFailure, List[Contact]]] = Sync[F].pure(
    Left(
      DecodingFailure(
        s"Unexpected Command for Op(${opId.value})"
      )
    )
  )

  private def parsePayloadDataToContacts(
      response: KadResponsePackage
  ): F[Either[DecodingFailure, List[Contact]]] =
    Sync[F].delay {
      val payloadData: Array[Byte] = response.payload.data
      payloadData.toObject[Set[Contact]].map(_.toList)
    }

  private def collectContacts(
      result: List[Either[DecodingFailure, Contact]]
  ): List[Contact] = result
    .flatMap(_ match {
      case Right(c) => List(c)
      case _        => List()
    })

  private def collectErrors(
      result: List[Either[DecodingFailure, Contact]]
  ): List[DecodingFailure] = result
    .flatMap(_ match {
      case Left(error) => List(error)
      case _           => List()
    })

}
