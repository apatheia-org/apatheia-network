package org.apatheia.network.algorithm.findvalue

import cats.effect.kernel.Async
import cats.implicits._
import org.apatheia.algorithm.findvalue.FindValueAlgorithm
import org.apatheia.error.FindValueError
import org.apatheia.model.FindValuePayload
import org.apatheia.model.NodeId
import org.apatheia.network.meta.LocalhostMetadataRef
import org.apatheia.store.ApatheiaKeyValueStore

class SubscriberFindValueAlgorithm[F[_]: Async](
    apatheiaKeyValueStore: ApatheiaKeyValueStore[NodeId, Array[Byte]],
    localhostMetadataRef: LocalhostMetadataRef[F]
) extends FindValueAlgorithm[F] {
  override def findValue(
      targetId: NodeId
  ): F[FindValueAlgorithm.FindValueResult] =
    localhostMetadataRef.get.map(localhostMetadata =>
      apatheiaKeyValueStore
        .get(targetId)
        .map(data =>
          Right(
            FindValuePayload(
              contact = localhostMetadata.localContact,
              value = Some(data)
            )
          )
        )
        .getOrElse(
          Left(
            FindValueError(
              message =
                s"Data for Node ${targetId} was not found in local storage of this host",
              contacts =
                localhostMetadata.routingTable.findClosestContacts(targetId)
            )
          )
        )
    )
}
