package org.apatheia.network.model

import org.apatheia.model.NodeId
import org.apatheia.model.PackageData
import org.apatheia.error.PackageDataParsingError
import org.apatheia.network.model.BytesizedPackageData._

final case class KadResponseHeaders(from: NodeId, to: NodeId, opId: OpId)
    extends PackageData {

  override def toByteArray: Array[Byte] =
    Array.concat(from.toByteArray, to.toByteArray, opId.toByteArray)

}

object KadResponseHeaders extends DefaultBytesizedParser[KadResponseHeaders] {
  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, KadResponseHeaders] = for {
    from <- NodeId.parse(byteArray.take(NodeId.byteSize))
    to <- NodeId.parse(byteArray.drop(NodeId.byteSize).take(NodeId.byteSize))
    opId <- OpId.parse(
      byteArray.drop(2 * NodeId.byteSize).take(OpId.byteSize)
    )
  } yield (KadResponseHeaders(from, to, opId))

  override val byteSize: Int = NodeId.byteSize + NodeId.byteSize + OpId.byteSize
}
