package org.apatheia.network.model

import java.util.UUID
import org.apatheia.model.PackageData
import java.nio.ByteBuffer
import org.apatheia.error.PackageDataParsingError
import cats.implicits._
import scala.util.Try

final case class OpId(value: UUID) extends PackageData {
  override def toByteArray: Array[Byte] = ByteBuffer
    .wrap(new Array[Byte](OpId.byteSize))
    .putLong(value.getMostSignificantBits())
    .putLong(value.getLeastSignificantBits())
    .array()
}

object OpId extends DefaultBytesizedParser[OpId] {

  override val byteSize: Int = 16

  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, OpId] = {
    val byteBuffer: ByteBuffer = ByteBuffer.wrap(byteArray)
    Try(new UUID(byteBuffer.getLong(), byteBuffer.getLong())).toEither
      .flatMap(uuid => Right(OpId(uuid)))
      .leftFlatMap(_ =>
        Left(PackageDataParsingError("Error while parsing OpId corrupt data"))
      )
  }

  def random: OpId = OpId(UUID.randomUUID())

}
