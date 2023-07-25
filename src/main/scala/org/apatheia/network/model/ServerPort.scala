package org.apatheia.network.model

import java.nio.ByteBuffer
import scala.util.Try
import cats.syntax.either._
import org.apatheia.codec.{Encoder, Decoder}
import org.apatheia.codec.DecodingFailure

final case class ServerPort(value: Int)

object ServerPort {
  val byteSize = 4

  implicit val encoder: Encoder[ServerPort] = new Encoder[ServerPort] {
    override def toByteArray(s: ServerPort): Array[Byte] =
      ByteBuffer.allocate(ServerPort.byteSize).putInt(s.value).array()
  }

  implicit val decoder: Decoder[ServerPort] = new Decoder[ServerPort] {
    override def toObject(
        data: Array[Byte]
    ): Either[DecodingFailure, ServerPort] =
      Try(ByteBuffer.wrap(data).getInt()).toEither
        .flatMap(port => Right(ServerPort(port)))
        .leftFlatMap(_ =>
          Left(
            DecodingFailure("Error while parsing ServerPort corrupt data")
          )
        )
  }

}
