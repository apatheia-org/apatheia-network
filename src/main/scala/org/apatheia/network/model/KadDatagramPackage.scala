package org.apatheia.network.model

import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure
import org.apatheia.codec.Encoder
import org.apatheia.codec.Decoder

case class KadDatagramPackage(
    headers: KadRequestHeaders,
    payload: KadDatagramPayload
)

object KadDatagramPackage {
  implicit val decoder: Decoder[KadDatagramPackage] =
    new Decoder[KadDatagramPackage] {
      override def toObject(
          data: Array[Byte]
      ): Either[DecodingFailure, KadDatagramPackage] = for {
        headers <- data
          .take(KadRequestHeaders.byteSize)
          .toObject[KadRequestHeaders]
        payload <- data
          .drop(KadRequestHeaders.byteSize)
          .toObject[KadDatagramPayload]
      } yield (KadDatagramPackage(headers, payload))
    }

  implicit val encoder: Encoder[KadDatagramPackage] =
    new Encoder[KadDatagramPackage] {
      override def toByteArray(k: KadDatagramPackage): Array[Byte] =
        Array.concat(k.headers.toByteArray, k.payload.toByteArray)
    }
}
