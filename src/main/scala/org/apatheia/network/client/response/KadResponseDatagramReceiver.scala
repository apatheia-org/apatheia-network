package org.apatheia.network.client.response

import cats.effect.kernel.Async
import org.apatheia.network.server.UDPDatagramReceiver
import org.apatheia.network.model.UDPDatagram
import org.apatheia.store.ApatheiaKeyValueStore

final case class KadResponseDatagramReceiver[F[_]: Async](
    keyStore: ApatheiaKeyValueStore
) extends UDPDatagramReceiver[F] {
  override def onUDPDatagramReceived(udpDatagram: UDPDatagram): F[Unit] = ???
}
