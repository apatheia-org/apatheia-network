package org.apatheia.network.client.response

import cats.effect.kernel.Async
import org.apatheia.network.server.UDPDatagramReceiver
import org.apatheia.network.model.UDPDatagram
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.client.response.store.ResponseStoreRef
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.implicits._
import org.apatheia.codec.Codec._

final case class KadResponseDatagramReceiver[F[_]: Async](
    responseKeyStore: ResponseStoreRef[F]
) extends UDPDatagramReceiver[F] {

  private val logger = Slf4jLogger.getLogger[F]

  private def processPackage(kadPackage: KadResponsePackage): F[Unit] =
    responseKeyStore.store(kadPackage.headers.opId, kadPackage)

  override def onUDPDatagramReceived(udpDatagram: UDPDatagram): F[Unit] =
    logger.debug(s"Receiving response datagram: ${udpDatagram.from}") *>
      (udpDatagram.data.toObject[KadResponsePackage] match {
        case Right(kadPackage) => processPackage(kadPackage)
        case Left(error)       => logger.error(error.message)
      })
}
