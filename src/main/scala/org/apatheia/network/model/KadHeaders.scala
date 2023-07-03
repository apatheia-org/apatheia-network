package org.apatheia.network.model

import org.apatheia.model.NodeId
import org.apatheia.model.PackageData
import org.apatheia.model.PackageDataParser
import org.apatheia.error.PackageDataParsingError
import org.apatheia.network.model.BytesizedPackageData._

final case class KadHeaders(
    from: NodeId,
    to: NodeId,
    opId: OpId,
    responseServerPort: Option[ServerPort] = None
) extends PackageData
    with BytesizedPackageData {

  override def toByteArray: Array[Byte] =
    Array.concat(
      from.toByteArray,
      to.toByteArray,
      opId.toByteArray,
      responseServerPort.map(_.toByteArray).getOrElse(Array.empty)
    )

  val byteSize: Int = toByteArray.length

}

object KadHeaders extends PackageDataParser[KadHeaders] {
  override def parse(
      byteArray: Array[Byte]
  ): Either[PackageDataParsingError, KadHeaders] = for {
    from <- NodeId.parse(byteArray.take(NodeId.byteSize))
    to <- NodeId.parse(byteArray.drop(NodeId.byteSize).take(NodeId.byteSize))
    opId <- OpId.parse(
      byteArray.drop(2 * NodeId.byteSize).take(OpId.byteSize)
    )
    responseServerPort <-
      if (byteArray.size > partialByteSize) {
        ServerPort
          .parse(
            byteArray
              .drop(2 * NodeId.byteSize + OpId.byteSize)
              .take(ServerPort.byteSize)
          )
          .map(Some.apply)
      } else { Right(None) }

  } yield (KadHeaders(from, to, opId, responseServerPort))

  private[model] val partialByteSize: Int =
    NodeId.byteSize + NodeId.byteSize + OpId.byteSize

  private[model] val fullByteSize: Int =
    NodeId.byteSize + NodeId.byteSize + OpId.byteSize + ServerPort.byteSize

}
