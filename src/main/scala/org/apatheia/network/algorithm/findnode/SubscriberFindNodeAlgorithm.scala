package org.apatheia.algorithm.findnode.sub

import cats.effect.kernel.Async
import org.apatheia.model.NodeId
import org.apatheia.model.RoutingTable
import org.apatheia.model.Contact
import org.apatheia.algorithm.findnode.FindNodeAlgorithm

final case class SubscriberFindNodeAlgorithm[F[_]: Async]()
    extends FindNodeAlgorithm[F] {
  override def findNode(
      routingTable: RoutingTable,
      target: NodeId
  ): F[Set[Contact]] =
    Async[F].pure(routingTable.findClosestContacts(targetId = target).toSet)
}
