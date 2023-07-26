package org.apatheia.network.client

import org.apatheia.algorithm.findvalue.FindValueAlgorithm
import org.apatheia.model.Contact
import org.apatheia.model.NodeId

trait FindValueClient[F[_]] {
  def sendFindValue(
      targetId: NodeId,
      contact: Contact
  ): F[FindValueAlgorithm.FindValueResult]
}
