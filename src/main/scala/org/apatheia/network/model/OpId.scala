package org.apatheia.network.model

import java.util.UUID
import java.nio.ByteBuffer
import cats.implicits._
import scala.util.Try
import org.apatheia.codec.{Encoder, Decoder}
import org.apatheia.codec.DecodingFailure

final case class OpId(value: UUID)

object OpId {

  val byteSize: Int = 16

  implicit val decoder: Decoder[OpId] = new Decoder[OpId] {
    override def toObject(data: Array[Byte]): Either[DecodingFailure, OpId] = {
      val byteBuffer: ByteBuffer = ByteBuffer.wrap(data)
      Try(new UUID(byteBuffer.getLong(), byteBuffer.getLong())).toEither
        .flatMap(uuid => Right(OpId(uuid)))
        .leftFlatMap(_ =>
          Left(DecodingFailure("Error while parsing OpId corrupt data"))
        )
    }
  }

  implicit val encoder: Encoder[OpId] = new Encoder[OpId] {
    override def toByteArray(opId: OpId): Array[Byte] = ByteBuffer
      .wrap(new Array[Byte](OpId.byteSize))
      .putLong(opId.value.getMostSignificantBits())
      .putLong(opId.value.getLeastSignificantBits())
      .array()

  }

  def random: OpId = OpId(UUID.randomUUID())

}
