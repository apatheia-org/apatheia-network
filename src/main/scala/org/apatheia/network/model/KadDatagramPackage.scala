package org.apatheia.network.model

import org.apatheia.model.PackageData
import org.apatheia.model.PackageDataParser
import org.apatheia.error.PackageDataParsingError
import org.apatheia.network.model.UDPDatagramParser

case class KadDatagramPackage(
    headers: KadHeaders,
    payload: KadDatagramPayload,
    udpDatagram: UDPDatagram
) extends PackageData {
  override def toByteArray: Array[Byte] =
    Array.concat(headers.toByteArray, payload.toByteArray)
}

object KadDatagramPackage extends UDPDatagramParser[KadDatagramPackage] {
  override def parse(
      udpDatagram: UDPDatagram
  ): Either[PackageDataParsingError, KadDatagramPackage] = for {
    headers <- KadHeaders.parse(udpDatagram.data.take(KadHeaders.byteSize))
    payload <- KadDatagramPayload.parse(
      udpDatagram.data.drop(KadHeaders.byteSize)
    )
  } yield (KadDatagramPackage(headers, payload, udpDatagram))
}
