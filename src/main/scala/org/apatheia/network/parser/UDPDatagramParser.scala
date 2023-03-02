package org.apatheia.network.parser

import org.apatheia.network.model.UDPDatagram

trait UDPDatagramParser[T] {
  def parse(udpDatagram: UDPDatagram): T
}
