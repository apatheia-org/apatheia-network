package org.apatheia.network.server

import org.apatheia.network.model.UDPDatagram

trait UDPDatagramReceiver[F[_]] {
  def onUDPDatagramReceived(udpDatagram: UDPDatagram): F[Unit]
}
