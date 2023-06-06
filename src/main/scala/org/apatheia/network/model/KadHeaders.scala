package org.apatheia.network.model

import org.apatheia.model.NodeId
import org.apatheia.model.PackageData
import org.apatheia.model.PackageDataParser
import org.apatheia.error.PackageDataParsingError

final case class KadHeaders(
    from: NodeId,
    to: NodeId,
    opId: OpId,
    responseServerPort: Option[ServerPort] = None
) extends PackageData {

  override def toByteArray: Array[Byte] =
    Array.concat(
      from.toByteArray,
      to.toByteArray,
      opId.toByteArray,
      responseServerPort.map(_.toByteArray).getOrElse(Array.empty)
    )

  val byteSize: Int =
    responseServerPort
      .map(_ => KadHeaders.fullByteSize)
      .getOrElse(KadHeaders.partialByteSize)

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
    responseServerPort <-
      if (byteArray.size > partialByteSize) {
        ServerPort
          .parse(
            byteArray
              .drop(2 * NodeId.BYTESIZE + OpId.BYTESIZE)
              .take(ServerPort.BYTESIZE)
          )
          .map(Some.apply)
      } else { Right(None) }

  } yield (KadHeaders(from, to, opId, responseServerPort))

  val partialByteSize: Int =
    NodeId.BYTESIZE + NodeId.BYTESIZE + OpId.BYTESIZE

  val fullByteSize: Int =
    NodeId.BYTESIZE + NodeId.BYTESIZE + OpId.BYTESIZE + ServerPort.BYTESIZE

}
