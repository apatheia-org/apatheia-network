package org.apatheia.network.model

import org.apatheia.error.PackageDataParsingError

trait UDPDatagramParser[T] {
  def parse(udpDatagram: UDPDatagram): Either[PackageDataParsingError, T]
}
