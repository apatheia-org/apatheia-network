package org.apatheia.network.model

import org.apatheia.model.PackageData
import org.apatheia.model.PackageDataParser
import org.apatheia.error.PackageDataParsingError

final case class KadDatagramPayload(
    command: KadCommand,
    data: Array[Byte]
) extends PackageData {
  override def toByteArray: Array[Byte] =
    Array.concat(command.toByteArray, data)
}

object KadDatagramPayload extends PackageDataParser[KadDatagramPayload] {
  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, KadDatagramPayload] =
    KadCommand
      .parse(Array(byteArray.head))
      .flatMap(command => Right(KadDatagramPayload(command, byteArray.drop(1))))

}
