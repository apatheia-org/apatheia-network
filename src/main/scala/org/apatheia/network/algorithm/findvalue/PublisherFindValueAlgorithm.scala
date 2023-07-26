package org.apatheia.network.algorithm.findvalue

import cats.effect.kernel.Async
import cats.implicits._
import org.apatheia.algorithm.findvalue.FindValueAlgorithm
import org.apatheia.algorithm.findvalue.FindValueAlgorithm.FindValueResult
import org.apatheia.error.FindValueError
import org.apatheia.model.Contact
import org.apatheia.model.FindValuePayload
import org.apatheia.model.NodeId
import org.apatheia.network.client.FindValueClient
import org.apatheia.network.meta.LocalhostMetadataRef

case class PublisherFindValueAlgorithm[F[_]: Async](
    findValueClient: FindValueClient[F],
    localhostMetadataRef: LocalhostMetadataRef[F],
    maxIterations: Int = 20
) extends FindValueAlgorithm[F] {

  override def findValue(
      targetId: NodeId
  ): F[FindValueResult] =
    localhostMetadataRef.get.flatMap(localhostMetadataRef =>
      findValueRecursive(
        targetId = targetId,
        contacts =
          localhostMetadataRef.routingTable.findClosestContacts(targetId)
      )(maxIterations)
    )

  private def findValueRecursive(
      targetId: NodeId,
      contacts: Set[Contact]
  )(iterations: Int): F[FindValueResult] =
    if (iterations == 0) {
      Async[F].pure(
        Either.left[FindValueError, FindValuePayload](
          FindValueError(message = "NOT FOUND", contacts = Set.empty)
        )
      )
    } else {
      sendFindValueRequest(targetId, contacts).flatMap { responses =>
        responses
          .collectFirst { case Right(responsePayload) => responsePayload }
          .map(successfulResponse =>
            Async[F].pure(
              Either.right[FindValueError, FindValuePayload](successfulResponse)
            )
          )
          .getOrElse {
            val retryContacts: Set[Contact] =
              responses
                .collect { case Left(retryContacts) => retryContacts }
                .toSet[FindValueError]
                .flatMap(_.contacts)

            localhostMetadataRef
              .updateRoutingTable(
                contacts = retryContacts
              )
              .flatMap(_ =>
                findValueRecursive(targetId, retryContacts)(iterations - 1)
              )
          }
      }
    }

  private def sendFindValueRequest(
      targetId: NodeId,
      contacts: Set[Contact]
  ): F[List[FindValueResult]] =
    contacts.toList.traverse(contact =>
      findValueClient.sendFindValue(targetId, contact)
    )

}
