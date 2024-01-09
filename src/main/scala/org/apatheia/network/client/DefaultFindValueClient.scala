package org.apatheia.network.client

import cats.effect.kernel.Async
import cats.implicits._
import org.apatheia.algorithm.findvalue.FindValueAlgorithm
import org.apatheia.codec.Codec._
import org.apatheia.error.FindValueError
import org.apatheia.model.Contact
import org.apatheia.model.FindValuePayload
import org.apatheia.model.NodeId
import org.apatheia.network.client.FindValueClient
import org.apatheia.network.client.UDPClient
import org.apatheia.network.client.response.consumer.KadResponseConsumer
import org.apatheia.network.meta.LocalhostMetadataRef
import org.apatheia.network.model.Codecs.FindValueErrorCodec._
import org.apatheia.network.model.Codecs.FindValuePayloadCodec._
import org.apatheia.network.model.Codecs.NodeIdCodec._
import org.apatheia.network.model.KadCommand
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.KadDatagramPayload
import org.apatheia.network.model.KadRequestHeaders
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.model.OpId

import java.net.InetSocketAddress
import scala.concurrent.duration.Duration

class DefaultFindValueClient[F[_]: Async](
    localhostMetadataRef: LocalhostMetadataRef[F],
    udpClient: UDPClient[F],
    kadResponseConsumer: KadResponseConsumer[F],
    responseTimeout: Duration
) extends FindValueClient[F] {

  override def sendFindValue(
      target: NodeId,
      remote: Contact
  ): F[FindValueAlgorithm.FindValueResult] = for {
    kadDatagramPackage <- toKadDatagramPackage(remote, target)
    _ <- udpClient.send(
      new InetSocketAddress(remote.ip, remote.port),
      kadDatagramPackage.toByteArray
    )
    response <- kadResponseConsumer.consumeResponse(
      opId = kadDatagramPackage.headers.opId,
      timeout = responseTimeout
    )
    result <- toFindValueResponse(response)
  } yield (result)

  private def toKadDatagramPackage(
      remote: Contact,
      target: NodeId
  ): F[KadDatagramPackage] = localhostMetadataRef.get.map { localhostMetadata =>
    KadDatagramPackage(
      headers = KadRequestHeaders(
        from = localhostMetadata.localContact.nodeId,
        to = remote.nodeId,
        opId = OpId.random,
        responseServerPort = localhostMetadata.responseServerPort
      ),
      payload = KadDatagramPayload(
        command = KadCommand.FindValue,
        data = target.toByteArray
      )
    )
  }

  private def toFindValueResponse(
      responsePackage: Option[KadResponsePackage]
  ): F[FindValueAlgorithm.FindValueResult] = Async[F].delay(
    responsePackage match {
      case Some(response) =>
        response.payload.data
          .toObject[FindValuePayload]
          .leftFlatMap(e => {
            Either.left(response.payload.data.toObject[FindValueError] match {
              case Left(decodingError) =>
                FindValueError(
                  message = decodingError.message,
                  contacts = Set.empty
                )
              case Right(error) => error
            })
          })

      case _ => Left(FindValueError("EMPTY RESPONSE", Set.empty))
    }
  )

}
