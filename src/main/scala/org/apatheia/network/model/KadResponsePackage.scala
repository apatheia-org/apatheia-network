package org.apatheia.network.model

import org.apatheia.error.PackageDataParsingError
import org.apatheia.network.model.UDPDatagramParser
import org.apatheia.model.PackageData

final case class KadResponsePackage(
    headers: KadResponseHeaders,
    payload: KadResponsePayload
) extends PackageData {
  override def toByteArray: Array[Byte] =
    Array.concat(headers.toByteArray, payload.toByteArray)
}

object KadResponsePackage extends UDPDatagramParser[KadResponsePackage] {

  override def parse(
      udpDatagram: UDPDatagram
  ): Either[PackageDataParsingError, KadResponsePackage] = for {
    headers <- KadResponseHeaders.parse(
      udpDatagram.data.take(KadResponseHeaders.byteSize)
    )
    payload <- KadResponsePayload.parse(
      udpDatagram.data.drop(KadResponseHeaders.byteSize)
    )
  } yield (KadResponsePackage(headers, payload))

}
