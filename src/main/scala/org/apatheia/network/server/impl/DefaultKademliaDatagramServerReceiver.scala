package org.apatheia.network.server.impl

import cats.effect.kernel.Async
import org.apatheia.network.server.UDPDatagramReceiver
import org.apatheia.network.model.UDPDatagram
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.KadCommand
import org.apatheia.network.server.KademliaServerProcessor
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.apatheia.network.meta.LocalhostMetadataRef
import org.apatheia.network.client.UDPClient
import org.apatheia.algorithm.findnode.sub.SubscriberFindNodeAlgorithm
import cats.implicits._
import org.apatheia.codec.Codec._

case class DefaultKademliaDatagramServerReceiver[F[_]: Async](
    localhostMetadataRef: LocalhostMetadataRef[F],
    requestServerClient: UDPClient[F]
) extends UDPDatagramReceiver[F] {

  private final val logger = Slf4jLogger.getLogger[F]
  private final val processorsMap: Map[KadCommand, KademliaServerProcessor[F]] =
    Map(
      KadCommand.FindNode -> FindNodeServerProcessor[F](
        localhostMetadataRef = localhostMetadataRef,
        findNodeSubscriberAlgorithm = SubscriberFindNodeAlgorithm[F](),
        requestServerClient = requestServerClient
      )
    )

  override def onUDPDatagramReceived(udpDatagram: UDPDatagram): F[Unit] =
    for {
      _ <- logger.debug(
        s"Processing incoming Kademlia UDP datagram: ${udpDatagram.from}"
      )
      result <- udpDatagram.data.toObject[KadDatagramPackage] match {
        case Left(e) =>
          logger.error(s"Error while parsing UDP Datagram(${udpDatagram.from
              .getHostName()}:${udpDatagram.from.getPort()}): ${e.message}")
        case Right(kadPackage) =>
          logger.debug(s"Parsed kademlia package: ${kadPackage}") *>
            processorsMap
              .get(kadPackage.payload.command)
              .map(_.process(kadPackage, udpDatagram))
              .getOrElse(
                logger.warn(
                  s"No command found for Kad Datagram(${kadPackage.headers})"
                )
              )
      }
    } yield (result)

}
