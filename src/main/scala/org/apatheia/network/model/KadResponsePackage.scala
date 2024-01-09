package org.apatheia.network.model

import org.apatheia.codec.Codec._
import org.apatheia.codec.Decoder
import org.apatheia.codec.DecodingFailure
import org.apatheia.codec.Encoder

final case class KadResponsePackage(
    headers: KadResponseHeaders,
    payload: KadResponsePayload
)

object KadResponsePackage {

  implicit val decoder: Decoder[KadResponsePackage] =
    new Decoder[KadResponsePackage] {
      override def toObject(
          data: Array[Byte]
      ): Either[DecodingFailure, KadResponsePackage] = for {
        headers <- data
          .take(KadResponseHeaders.byteSize)
          .toObject[KadResponseHeaders]
        payload <-
          data
            .drop(KadResponseHeaders.byteSize)
            .toObject[KadResponsePayload]

      } yield (KadResponsePackage(headers, payload))
    }

  implicit val encoder: Encoder[KadResponsePackage] =
    new Encoder[KadResponsePackage] {
      override def toByteArray(k: KadResponsePackage): Array[Byte] =
        Array.concat(k.headers.toByteArray, k.payload.toByteArray)
    }

}
