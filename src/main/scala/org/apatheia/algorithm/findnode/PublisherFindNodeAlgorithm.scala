package org.apatheia.algorithm.findnode

import org.apatheia.model.RoutingTable
import org.apatheia.model.NodeId
import org.apatheia.model.Contact
import cats.effect.kernel.Async
import cats.implicits._
import cats.Applicative
import org.apatheia.algorithm.findnode.FindNodeAlgorithm
import org.apatheia.algorithm.findnode.FindNodeClient

case class PublisherFindNodeAlgorithm[F[_]: Async: Applicative](
    findNodeClient: FindNodeClient[F],
    maxIterations: Int = 20
) extends FindNodeAlgorithm[F] {

  override def findNode(
      routingTable: RoutingTable,
      target: NodeId
  ): F[Set[Contact]] = {
    val closestContacts = routingTable.findClosestContacts(target)
    retryFindNodeRequest(
      iteration = maxIterations,
      routingTable = routingTable,
      closestContacts = closestContacts,
      target = target
    )
  }

  private[findnode] def retryFindNodeRequest(
      iteration: Int,
      routingTable: RoutingTable,
      closestContacts: List[Contact],
      target: NodeId
  ): F[Set[Contact]] =
    if (iteration == 0) {
      Async[F].pure(Set.empty)
    } else {
      (for {
        contacts <- filterFromTargetNode(closestContacts, target)
        requestContacts <- sendFindNodeRequests(contacts, target)
      } yield (requestContacts.toList
        .sortBy(
          _.nodeId.distance(target)
        )
        .take(routingTable.k))).flatMap { foundContacts =>
        if (foundContacts.isEmpty) {
          retryFindNodeRequest(
            iteration = iteration - 1,
            routingTable = routingTable,
            closestContacts = closestContacts,
            target = target
          )
          // TODO update local routing table with response
        } else {
          Async[F].pure(foundContacts.toSet)
        }
      }
    }

  private def filterFromTargetNode(
      closestContacts: List[Contact],
      target: NodeId
  ): F[Set[Contact]] =
    Async[F]
      .pure(
        closestContacts
          .filter(_.nodeId.value != target.value)
          .toSet[Contact]
      )

  private def sendFindNodeRequests(
      nodeContacts: Set[Contact],
      target: NodeId
  ): F[Set[Contact]] =
    nodeContacts.toList
      .map(contact => findNodeClient.requestContacts(contact, target))
      .flatTraverse(a => a)
      .flatMap(a => Async[F].pure((a.toSet[Contact])))

}
