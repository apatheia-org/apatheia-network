package org.apatheia.algorithm.findnode

import org.apatheia.model.Contact
import org.apatheia.network.client.response.consumer.KadResponseConsumer
import org.apatheia.network.model.OpId
import scala.concurrent.duration.Duration
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.client.UDPClient
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.KadDatagramPayload
import org.apatheia.network.model.KadCommand
import org.apatheia.model.NodeId
import cats.implicits._
import java.net.InetSocketAddress
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.apatheia.network.meta.LocalhostMetadataRef
import cats.effect.kernel.Sync
import org.apatheia.network.model.KadRequestHeaders
import org.apatheia.network.model.Tag
import org.apatheia.codec.Codec._
import org.apatheia.network.model.Codecs.NodeIdCodec._
import org.apatheia.network.model.Codecs.ContactCodec._
import org.apatheia.codec.DecodingFailure

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
      val indexes: Seq[Int] =
        findPatternIndexes(Tag.Contact.tagData, payloadData)
      val result: List[Either[DecodingFailure, Contact]] =
        slideContactsByIndexes(indexes, payloadData).toList

      toContactsResponse(result)
    }

  private def toContactsResponse(
      result: List[Either[DecodingFailure, Contact]]
  ): Either[DecodingFailure, List[Contact]] = {
    val errors = collectErrors(result)

    if (errors.isEmpty) {
      Right(collectContacts(result))
    } else {
      Left(
        DecodingFailure(
          s"Error while parsing response contacts:\n\n* ${errors.map(_.message).mkString("\n")}"
        )
      )
    }
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

  private def slideContactsByIndexes(
      indexes: Seq[Int],
      payloadData: Array[Byte]
  ): Iterator[Either[DecodingFailure, Contact]] = indexes match {
    case Seq(x) => Iterator(payloadData.drop(x).toObject[Contact])
    case _ =>
      indexes
        .sliding(2, 1)
        .map(pairSeq => {
          pairSeq match {
            case Seq(x, y) => {
              payloadData
                .slice(x, y - Tag.Contact.tagData.size)
                .toObject[Contact]
            }
          }
        })
        .concat(Iterator(payloadData.drop(indexes.last).toObject[Contact]))
  }

  private def findPatternIndexes(
      pattern: Array[Byte],
      data: Array[Byte]
  ): Seq[Int] = {
    val patternLength = pattern.length
    val dataLength = data.length

    if (patternLength > dataLength) {
      Seq.empty[Int]
    } else {
      val maxStartIndex = dataLength - patternLength
      val patternStartByte = pattern(0)

      (0 to maxStartIndex)
        .filter { i =>
          data(i) == patternStartByte && pattern.indices.forall(j =>
            data(i + j) == pattern(j)
          )
        }
        .map(
          _ + Tag.Contact.tagData.size
        )
    }
  }

}