package org.apatheia.network.model

import org.apatheia.codec.{Encoder, Decoder}
import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure

final case class KadResponsePayload(command: KadCommand, data: Array[Byte])

object KadResponsePayload {

  implicit val decoder: Decoder[KadResponsePayload] =
    new Decoder[KadResponsePayload] {
      override def toObject(
          data: Array[Byte]
      ): Either[DecodingFailure, KadResponsePayload] = data
        .take(1)
        .toObject[KadCommand]
        .flatMap(command => {
          Right(KadResponsePayload(command, data.drop(1)))
        })
    }

  implicit val encoder: Encoder[KadResponsePayload] =
    new Encoder[KadResponsePayload] {
      override def toByteArray(k: KadResponsePayload): Array[Byte] =
        Array.concat(k.command.toByteArray, k.data)
    }

}
