package org.apatheia.network.model

import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure
import org.apatheia.codec.Encoder
import org.apatheia.codec.Decoder

final case class KadDatagramPayload(
    command: KadCommand,
    data: Array[Byte]
)

object KadDatagramPayload {
  implicit val encoder: Encoder[KadDatagramPayload] =
    new Encoder[KadDatagramPayload] {
      override def toByteArray(k: KadDatagramPayload): Array[Byte] =
        Array.concat(k.command.toByteArray, k.data)
    }

  implicit val decoder: Decoder[KadDatagramPayload] =
    new Decoder[KadDatagramPayload] {
      override def toObject(
          data: Array[Byte]
      ): Either[DecodingFailure, KadDatagramPayload] = Array(data.head)
        .toObject[KadCommand]
        .flatMap(command => Right(KadDatagramPayload(command, data.drop(1))))

    }
}
