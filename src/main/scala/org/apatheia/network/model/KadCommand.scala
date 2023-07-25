package org.apatheia.network.model

import org.apatheia.codec.DecodingFailure
import org.apatheia.codec.Encoder
import org.apatheia.codec.Decoder

sealed abstract class KadCommand(val value: Byte)

object KadCommand {

  case object FindNode extends KadCommand(0)
  case object FindValue extends KadCommand(1)
  case object Store extends KadCommand(2)

  private val all: List[KadCommand] = List(FindNode, FindValue, Store)

  implicit val decoder: Decoder[KadCommand] = new Decoder[KadCommand] {
    override def toObject(
        data: Array[Byte]
    ): Either[DecodingFailure, KadCommand] = if (data.isEmpty) {
      Left(
        DecodingFailure(
          "Error while parsing command byte: empty byte value"
        )
      )
    } else {
      all.lift(data.head) match {
        case Some(command) => Right(command)
        case None =>
          Left(
            DecodingFailure(
              "Error while parsing command byte: invalid command byte"
            )
          )
      }
    }
  }

  implicit val encoder: Encoder[KadCommand] = new Encoder[KadCommand] {
    override def toByteArray(k: KadCommand): Array[Byte] = Array(k.value)
  }

}
