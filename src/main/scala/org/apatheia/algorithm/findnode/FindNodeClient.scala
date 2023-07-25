package org.apatheia.algorithm.findnode

import org.apatheia.model.Contact
import org.apatheia.model.NodeId

trait FindNodeClient[F[_]] {
  def requestContacts(remoteContact: Contact, target: NodeId): F[List[Contact]]
}
