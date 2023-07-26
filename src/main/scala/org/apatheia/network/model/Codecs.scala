package org.apatheia.network.model

import cats.syntax.either._
import org.apatheia.codec.Codec._
import org.apatheia.codec.Decoder
import org.apatheia.codec.DecodingFailure
import org.apatheia.codec.Encoder
import org.apatheia.error.FindValueError
import org.apatheia.model.Contact
import org.apatheia.model.FindValuePayload
import org.apatheia.model.NodeId
import org.apatheia.network.client.helpers.SeqIndex

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import scala.util.Try

object Codecs {

  val CHARSET: Charset = StandardCharsets.UTF_8

  implicit val stringDecoder: Decoder[String] = new Decoder[String] {
    override def toObject(
        data: Array[Byte]
    ): Either[DecodingFailure, String] = Right(new String(data, CHARSET))
  }

  implicit val stringEncoder: Encoder[String] = new Encoder[String] {
    override def toByteArray(str: String): Array[Byte] = str.getBytes(CHARSET)
  }

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
                CHARSET
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

  object FindValuePayloadCodec {
    import Codecs.ContactCodec._

    implicit val findValueEncoder: Encoder[FindValuePayload] =
      new Encoder[FindValuePayload] {
        override def toByteArray(t: FindValuePayload): Array[Byte] =
          Array.concat(
            t.contact.toByteArray,
            Tag.FindValueResponse.tagData,
            t.value.getOrElse(Array.empty)
          )
      }

    implicit val findValueDecoder: Decoder[FindValuePayload] =
      new Decoder[FindValuePayload] {
        override def toObject(
            data: Array[Byte]
        ): Either[DecodingFailure, FindValuePayload] = {
          val indexes: Seq[Int] =
            SeqIndex.findPatterns(Tag.FindValueResponse, data)

          if (indexes.size != 1) {
            Either.left(
              DecodingFailure(
                "Malformed FIND VALUE response payload: it should contain a single [FIND_VALUE_RESPONSE] tag"
              )
            )
          } else {
            indexes.headOption
              .map { tagIndex =>
                for {
                  contact <- data
                    .take(tagIndex - Tag.FindValueResponse.tagData.size)
                    .toObject[Contact]
                  payloadData <- Either.right(data.drop(tagIndex))
                } yield (
                  FindValuePayload(
                    contact = contact,
                    value = Some(payloadData)
                  )
                )
              }
              .getOrElse(
                Either.left(
                  DecodingFailure(
                    "Error while decoding serial into a FindValuePayload"
                  )
                )
              )
          }
        }
      }
  }

  object ContactSetCodec {
    import Codecs.ContactCodec._

    implicit val contactSetEncoder: Encoder[Set[Contact]] =
      new Encoder[Set[Contact]] {
        override def toByteArray(set: Set[Contact]): Array[Byte] =
          set.toArray.flatMap(c =>
            Array.concat(Tag.Contact.tagData, c.toByteArray)
          )
      }

    implicit val contactSetDecoder: Decoder[Set[Contact]] =
      new Decoder[Set[Contact]] {
        override def toObject(
            data: Array[Byte]
        ): Either[DecodingFailure, Set[Contact]] = {
          val contactIndexes = SeqIndex.findPatterns(Tag.Contact, data)
          val result: Set[Either[DecodingFailure, Contact]] = contactIndexes
            .sliding(2, 1)
            .map(pairSeq => {
              pairSeq match {
                case Seq(x) => data.drop(x).toObject[Contact]
                case Seq(x, y) => {
                  data
                    .slice(x, y - Tag.Contact.tagData.size)
                    .toObject[Contact]
                }
              }
            })
            .concat(Iterator(data.drop(contactIndexes.last).toObject[Contact]))
            .toSet
          val successfulResults: Set[Contact] = result.collect {
            case Right(contact) => contact
          }

          if (successfulResults.size < result.size) {
            val errors: Set[DecodingFailure] = result.collect {
              case Left(contact) => contact
            }
            Either.left(
              DecodingFailure(
                s"Errors while decoding a Set[Contact]: ${errors.mkString("\n")}"
              )
            )
          } else {
            Either.right(successfulResults)
          }
        }
      }

  }

  object FindValueErrorCodec {
    import ContactSetCodec._

    implicit val findValueErrorEncoder: Encoder[FindValueError] =
      new Encoder[FindValueError] {
        override def toByteArray(t: FindValueError): Array[Byte] =
          Array.concat(
            t.message.getBytes(CHARSET),
            t.contacts.toByteArray
          )
      }

    implicit val findValueErrorDecoder: Decoder[FindValueError] =
      new Decoder[FindValueError] {
        override def toObject(
            data: Array[Byte]
        ): Either[DecodingFailure, FindValueError] = {
          val firstTagIndex: Int = data.indexOfSlice(Tag.Contact.tagData.toSeq)
          for {
            message <- data.take(firstTagIndex).toObject[String]
            contacts <- data.drop(firstTagIndex).toObject[Set[Contact]]
          } yield (FindValueError(
            message = message,
            contacts = contacts
          ))
        }
      }
  }

}
