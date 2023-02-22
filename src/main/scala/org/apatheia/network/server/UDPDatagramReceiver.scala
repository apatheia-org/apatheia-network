package org.apatheia.network.server

import org.apatheia.network.model.UDPDatagram
import org.apache.mina.core.service.IoHandlerAdapter

trait UDPDatagramReceiver[F[_]] {
  def onUDPDatagramReceived(udpDatagram: UDPDatagram): F[Unit]
}
