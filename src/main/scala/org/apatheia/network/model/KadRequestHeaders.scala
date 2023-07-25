package org.apatheia.network.model

import org.apatheia.model.NodeId
import org.apatheia.codec.{Encoder, Decoder}
import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure
import org.apatheia.network.model.Codecs.NodeIdCodec
import org.apatheia.network.model.Codecs.NodeIdCodec._

final case class KadRequestHeaders(
    from: NodeId,
    to: NodeId,
    opId: OpId,
    responseServerPort: ServerPort
)

object KadRequestHeaders {

  val byteSize: Int =
    NodeIdCodec.BYTESIZE + NodeIdCodec.BYTESIZE + OpId.byteSize + ServerPort.byteSize

  implicit val encoder: Encoder[KadRequestHeaders] =
    new Encoder[KadRequestHeaders] {
      override def toByteArray(k: KadRequestHeaders): Array[Byte] =
        Array.concat(
          k.from.toByteArray,
          k.to.toByteArray,
          k.opId.toByteArray,
          k.responseServerPort.toByteArray
        )
    }

  implicit val decoder: Decoder[KadRequestHeaders] =
    new Decoder[KadRequestHeaders] {
      override def toObject(
          data: Array[Byte]
      ): Either[DecodingFailure, KadRequestHeaders] = for {
        from <- data.take(NodeIdCodec.BYTESIZE).toObject[NodeId]
        to <- data
          .drop(NodeIdCodec.BYTESIZE)
          .take(NodeIdCodec.BYTESIZE)
          .toObject[NodeId]
        opId <- data
          .drop(2 * NodeIdCodec.BYTESIZE)
          .take(OpId.byteSize)
          .toObject[OpId]
        responseServerPort <- data
          .drop(2 * NodeIdCodec.BYTESIZE + OpId.byteSize)
          .take(ServerPort.byteSize)
          .toObject[ServerPort]
      } yield (KadRequestHeaders(from, to, opId, responseServerPort))

    }
}
