package org.apatheia.network.model

import org.apatheia.model.NodeId
import org.apatheia.codec.Encoder
import org.apatheia.codec.Decoder
import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure
import org.apatheia.network.model.Codecs.NodeIdCodec
import org.apatheia.network.model.Codecs.NodeIdCodec._

final case class KadResponseHeaders(from: NodeId, to: NodeId, opId: OpId)

object KadResponseHeaders {

  implicit val encoder: Encoder[KadResponseHeaders] =
    new Encoder[KadResponseHeaders] {
      override def toByteArray(k: KadResponseHeaders): Array[Byte] =
        Array.concat(k.from.toByteArray, k.to.toByteArray, k.opId.toByteArray)
    }

  implicit val decoder: Decoder[KadResponseHeaders] =
    new Decoder[KadResponseHeaders] {
      override def toObject(
          data: Array[Byte]
      ): Either[DecodingFailure, KadResponseHeaders] = for {
        from <- data.take(NodeIdCodec.BYTESIZE).toObject[NodeId]
        to <- data
          .drop(NodeIdCodec.BYTESIZE)
          .take(NodeIdCodec.BYTESIZE)
          .toObject[NodeId]
        opId <- data
          .drop(2 * NodeIdCodec.BYTESIZE)
          .take(OpId.byteSize)
          .toObject[OpId]

      } yield (KadResponseHeaders(from, to, opId))

    }

  val byteSize: Int = 2 * NodeIdCodec.BYTESIZE + OpId.byteSize
}
