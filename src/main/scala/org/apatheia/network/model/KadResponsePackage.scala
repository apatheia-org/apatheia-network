package org.apatheia.network.model

import org.apatheia.error.PackageDataParsingError
import org.apatheia.network.model.UDPDatagramParser

final case class KadResponsePackage(
    headers: KadHeaders,
    payload: KadResponsePayload,
    udpDatagram: UDPDatagram
) extends UDPPackageData {
  override def toByteArray: Array[Byte] =
    Array.concat(headers.toByteArray, payload.toByteArray)
}

object KadResponsePackage extends UDPDatagramParser[KadResponsePackage] {

  override def parse(
      udpDatagram: UDPDatagram
  ): Either[PackageDataParsingError, KadResponsePackage] = for {
    headers <- KadHeaders.parse(
      udpDatagram.data.take(KadHeaders.partialByteSize)
    )
    payload <- KadResponsePayload.parse(
      udpDatagram.data.drop(headers.byteSize)
    )
  } yield (KadResponsePackage(headers, payload, udpDatagram))

}
