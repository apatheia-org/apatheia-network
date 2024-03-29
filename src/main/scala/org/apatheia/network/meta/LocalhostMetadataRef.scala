package org.apatheia.network.meta

import org.apatheia.model.Contact
import org.apatheia.network.model.LocalhostMetadata

trait LocalhostMetadataRef[F[_]] {
  def get: F[LocalhostMetadata]
  def modify(localhostMetadata: LocalhostMetadata): F[Unit]
  def updateRoutingTable(contacts: Set[Contact]): F[Unit]
}
