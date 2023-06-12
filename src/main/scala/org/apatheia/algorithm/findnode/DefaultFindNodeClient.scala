package org.apatheia.algorithm.findnode

import org.apatheia.model.Contact
import org.apatheia.network.client.response.consumer.KadResponseConsumer
import org.apatheia.network.model.OpId
import scala.concurrent.duration.Duration
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.client.UDPClient
import org.apatheia.network.model.KadHeaders
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.KadDatagramPayload
import org.apatheia.network.model.KadCommand
import org.apatheia.model.NodeId
import cats.implicits._
import java.net.InetSocketAddress
import org.apatheia.error.PackageDataParsingError
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.apatheia.network.model.tags.ContactTag
import org.apatheia.network.meta.LocalhostMetadataRef
import cats.effect.kernel.Sync

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
    val contactsResponse: F[Either[PackageDataParsingError, List[Contact]]] =
      for {
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

    // try to parse contacts response
    contactsResponse.flatMap {
      _ match {
        case Left(error) =>
          logger.error(error.message) *> Sync[F].pure(List.empty)
        case Right(contacts) => Sync[F].pure(contacts)
      }
    }
  }

  private def toKadDatagramPackage(
      remote: Contact,
      target: NodeId
  ): F[KadDatagramPackage] =
    defaultLocalhostMetadataRef.get.map(locahostMetadata =>
      KadDatagramPackage(
        headers = KadHeaders(
          from = locahostMetadata.localContact.nodeId,
          to = remote.nodeId,
          opId = OpId.random,
          responseServerPort = Some(locahostMetadata.responseServerPort)
        ),
        payload = KadDatagramPayload(
          command = KadCommand.FindNode,
          data = target.toByteArray
        )
      )
    )

  private def toContacts(
      responsePackage: Option[KadResponsePackage]
  ): F[Either[PackageDataParsingError, List[Contact]]] =
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
  ): F[Either[PackageDataParsingError, List[Contact]]] = Sync[F].pure(
    Left(
      PackageDataParsingError(
        s"Unexpected Command for Op(${opId.value})"
      )
    )
  )

  private def parsePayloadDataToContacts(
      response: KadResponsePackage
  ): F[Either[PackageDataParsingError, List[Contact]]] = {
    // Extract indexes of Contact tags
    val payloadData: Array[Byte] = response.payload.data
    val indexes: Seq[Int] =
      findPatternIndexes(
        ContactTag.tagData,
        payloadData
      )
    val result: List[Either[PackageDataParsingError, Contact]] =
      slideContactsByIndexes(indexes, payloadData).toList

    val errors: List[PackageDataParsingError] = collectErrors(result)

    // compute final response result
    Sync[F].pure(
      toContactsResponse(
        slideContactsByIndexes(indexes, payloadData).toList,
        errors
      )
    )
  }

  private def toContactsResponse(
      result: List[Either[PackageDataParsingError, Contact]],
      errors: List[PackageDataParsingError]
  ): Either[PackageDataParsingError, List[Contact]] = if (errors.isEmpty) {
    // weird gambiarra hunting arround
    Right(result.flatMap(_ match {
      case Right(contact) => {
        List(contact)
      }
      case Left(e) => {
        List()
      }
    }))
  } else {
    Left(
      PackageDataParsingError(
        s"Error while parsing response contacts:\n\n ${errors.map(_.message).mkString("\n")}"
      )
    )
  }

  private def collectErrors(
      result: List[Either[PackageDataParsingError, Contact]]
  ): List[PackageDataParsingError] = result
    .flatMap(_ match {
      case Left(error) => List(error)
      case _           => List()
    })

  private def slideContactsByIndexes(
      indexes: Seq[Int],
      payloadData: Array[Byte]
  ): Iterator[Either[PackageDataParsingError, Contact]] = {
    indexes
      .sliding(2, 1)
      .map(pairSeq => {
        pairSeq match {
          case Seq(x) => {
            Contact.parse(payloadData.drop(x))
          }
          case Seq(x, y) => {
            Contact.parse(payloadData.slice(x, y))
          }
        }
      })
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
          _ + ContactTag.tagData.size
        ) // gambiarra with potential of improvements
    }
  }

}
