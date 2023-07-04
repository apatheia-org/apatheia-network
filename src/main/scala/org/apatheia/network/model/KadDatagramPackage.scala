package org.apatheia.network.model

import org.apatheia.model.PackageData
import org.apatheia.error.PackageDataParsingError
import org.apatheia.network.model.UDPDatagramParser

case class KadDatagramPackage(
    headers: KadRequestHeaders,
    payload: KadDatagramPayload
) extends PackageData {
  override def toByteArray: Array[Byte] =
    Array.concat(headers.toByteArray, payload.toByteArray)
}

object KadDatagramPackage extends UDPDatagramParser[KadDatagramPackage] {
  override def parse(
      udpDatagram: UDPDatagram
  ): Either[PackageDataParsingError, KadDatagramPackage] = for {
    headers <- KadRequestHeaders.parse(
      udpDatagram.data.take(KadRequestHeaders.byteSize)
    )
    payload <- KadDatagramPayload.parse(
      udpDatagram.data.drop(KadRequestHeaders.byteSize)
    )
  } yield (KadDatagramPackage(headers, payload))
}
