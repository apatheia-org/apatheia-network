package org.apatheia.network.model

import java.nio.ByteBuffer
import org.apatheia.model.NodeId
import org.apatheia.codec.Decoder
import org.apatheia.codec.Encoder
import org.apatheia.model.Contact
import org.apatheia.codec.DecodingFailure
import scala.util.Try
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import cats.syntax.either._
import org.apatheia.codec.Codec._

object Codecs {

  val CHARSET: Charset = StandardCharsets.UTF_8

  object ContactCodec {
    import Codecs.NodeIdCodec._

    private val portByteSize: Int = 8

    implicit val contactDecoder: Decoder[Contact] = new Decoder[Contact] {
      override def toObject(
          data: Array[Byte]
      ): Either[DecodingFailure, Contact] = data
        .take(Codecs.NodeIdCodec.BYTESIZE)
        .toObject[NodeId]
        .flatMap(nodeId =>
          Try(
            Contact(
              nodeId = nodeId,
              port = ByteBuffer
                .wrap(
                  data
                    .slice(
                      Codecs.NodeIdCodec.BYTESIZE,
                      Codecs.NodeIdCodec.BYTESIZE + portByteSize
                    )
                )
                .getInt(),
              ip = new String(
                data.drop(Codecs.NodeIdCodec.BYTESIZE + portByteSize),
                StandardCharsets.UTF_8
              )
            )
          ).toEither.leftFlatMap(e =>
            Left(
              DecodingFailure(
                s"Unexpected error while parsing a byte array into a Contact: ${e}"
              )
            )
          )
        )
    }

    implicit val contactEncoder: Encoder[Contact] = new Encoder[Contact] {
      override def toByteArray(contact: Contact): Array[Byte] = Array.concat(
        contact.nodeId.toByteArray,
        ByteBuffer.allocate(8).putInt(contact.port).array(),
        contact.ip.getBytes(CHARSET)
      )
    }
  }

  object NodeIdCodec {

    final val BYTESIZE: Int = 20

    implicit val nodeIdDecoder: Decoder[NodeId] = new Decoder[NodeId] {
      override def toObject(
          data: Array[Byte]
      ): Either[DecodingFailure, NodeId] = Try(
        BigInt(new String(data, CHARSET).trim())
      ).toEither
        .flatMap(bigIntValue => Right(NodeId(bigIntValue)))
        .leftFlatMap(e =>
          Left(
            DecodingFailure(
              s"Unexpected error while parsing a byte array into a NodeId: ${e}"
            )
          )
        )
    }

    implicit val nodeIdEncoder: Encoder[NodeId] = new Encoder[NodeId] {
      override def toByteArray(n: NodeId): Array[Byte] = ByteBuffer
        .allocate(BYTESIZE)
        .put(n.value.toString().getBytes(CHARSET))
        .array()
    }
  }

}
