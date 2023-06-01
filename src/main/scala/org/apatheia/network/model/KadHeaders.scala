package org.apatheia.network.model

import org.apatheia.model.NodeId
import org.apatheia.model.PackageData
import org.apatheia.model.PackageDataParser
import org.apatheia.error.PackageDataParsingError

final case class KadHeaders(
    from: NodeId,
    to: NodeId,
    opId: OpId
) extends PackageData {

  override def toByteArray: Array[Byte] =
    Array.concat(from.toByteArray, to.toByteArray, opId.toByteArray)
}

object KadHeaders extends PackageDataParser[KadHeaders] {
  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, KadHeaders] = for {
    from <- NodeId.parse(byteArray.take(NodeId.BYTESIZE))
    to <- NodeId.parse(byteArray.drop(NodeId.BYTESIZE).take(NodeId.BYTESIZE))
    opId <- OpId.parse(
      byteArray.drop(2 * NodeId.BYTESIZE).take(OpId.BYTESIZE)
    )
  } yield (KadHeaders(from, to, opId))

  def byteSize =
    20 + 20 + 16 // 20(20 * 8) + 20(20 * 8) + 16(16 * 8) = 56(448 bits)

}
