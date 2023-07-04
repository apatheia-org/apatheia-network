package org.apatheia.network.model

import org.apatheia.model.NodeId
import org.apatheia.model.PackageData
import org.apatheia.error.PackageDataParsingError
import org.apatheia.network.model.BytesizedPackageData._
import org.apatheia.model.PackageDataParser

final case class KadRequestHeaders(
    from: NodeId,
    to: NodeId,
    opId: OpId,
    responseServerPort: ServerPort
) extends PackageData {

  override def toByteArray: Array[Byte] =
    Array.concat(
      from.toByteArray,
      to.toByteArray,
      opId.toByteArray,
      responseServerPort.toByteArray
    )

}

object KadRequestHeaders extends DefaultBytesizedParser[KadRequestHeaders] {
  override val byteSize: Int =
    NodeId.byteSize + NodeId.byteSize + OpId.byteSize + ServerPort.byteSize

  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, KadRequestHeaders] = for {
    from <- NodeId.parse(byteArray.take(NodeId.byteSize))
    to <- NodeId.parse(byteArray.drop(NodeId.byteSize).take(NodeId.byteSize))
    opId <- OpId.parse(
      byteArray.drop(2 * NodeId.byteSize).take(OpId.byteSize)
    )
    responseServerPort <- ServerPort.parse(
      byteArray
        .drop(2 * NodeId.byteSize + OpId.byteSize)
        .take(ServerPort.byteSize)
    )
  } yield (KadRequestHeaders(from, to, opId, responseServerPort))

}
