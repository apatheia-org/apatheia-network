package org.apatheia.network.client.response

import cats.effect.kernel.Async
import org.apatheia.network.server.UDPDatagramReceiver
import org.apatheia.network.model.UDPDatagram
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.client.response.store.ResponseStoreRef
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.KadCommand.FindNode
import org.apatheia.algorithm.findnode.pub.FindNodeAlgorithm

final case class KadResponseDatagramReceiver[F[_]: Async](
    responseKeyStore: ResponseStoreRef[F]
) extends UDPDatagramReceiver[F] {

  private val logger = Slf4jLogger.getLogger[F]

  private def processPackage(kadPackage: KadResponsePackage): F[Unit] =
    responseKeyStore.store(kadPackage.headers.opId, kadPackage)

  override def onUDPDatagramReceived(udpDatagram: UDPDatagram): F[Unit] =
    KadResponsePackage.parse(udpDatagram) match {
      case Right(kadPackage) => processPackage(kadPackage)
      case Left(error)       => logger.error(error.message)
    }
}
