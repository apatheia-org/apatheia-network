package org.apatheia.network.model

import org.apatheia.model.PackageData
import java.nio.ByteBuffer
import org.apatheia.error.PackageDataParsingError
import scala.util.Try
import cats.syntax.either._

final case class ServerPort(value: Int) extends PackageData {
  override def toByteArray: Array[Byte] =
    ByteBuffer.allocate(ServerPort.byteSize).putInt(value).array()

}

object ServerPort extends DefaultBytesizedParser[ServerPort] {
  val byteSize = 4

  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, ServerPort] =
    Try(ByteBuffer.wrap(byteArray).getInt()).toEither
      .flatMap(port => Right(ServerPort(port)))
      .leftFlatMap(_ =>
        Left(
          PackageDataParsingError("Error while parsing ServerPort corrupt data")
        )
      )

}
