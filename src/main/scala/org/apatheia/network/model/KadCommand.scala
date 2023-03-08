package org.apatheia.network.model

import org.apatheia.model.PackageData
import org.apatheia.model.PackageDataParser
import org.apatheia.error.PackageDataParsingError

sealed abstract class KadCommand(val value: Byte) extends PackageData {

  override def toByteArray: Array[Byte] = Array(value)

}

object KadCommand extends PackageDataParser[KadCommand] {

  case object FindNode extends KadCommand(0)
  case object FindValue extends KadCommand(1)
  case object Store extends KadCommand(2)

  private val all: List[KadCommand] = List(FindNode, FindValue, Store)

  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, KadCommand] = if (byteArray.isEmpty) {
    Left(
      PackageDataParsingError(
        "Error while parsing command byte: empty byte value"
      )
    )
  } else {
    all.lift(byteArray.head) match {
      case Some(command) => Right(command)
      case None =>
        Left(
          PackageDataParsingError(
            "Error while parsing command byte: invalid command byte"
          )
        )
    }
  }
}
