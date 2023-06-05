package org.apatheia.network.model

import org.apatheia.model.Contact
import org.apatheia.model.PackageData
import org.apatheia.model.PackageDataParser
import org.apatheia.error.PackageDataParsingError

final case class KadResponsePayload(command: KadCommand, data: Array[Byte])
    extends PackageData {
  override def toByteArray: Array[Byte] =
    Array.concat(command.toByteArray, data)
}

object KadResponsePayload extends PackageDataParser[KadResponsePayload] {
  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, KadResponsePayload] =
    KadCommand
      .parse(byteArray.take(1))
      .flatMap(command => {
        Right(KadResponsePayload(command, byteArray.drop(1)))
      })

}
