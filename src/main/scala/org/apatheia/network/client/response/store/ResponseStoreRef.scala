package org.apatheia.network.client.response.store

import org.apatheia.network.model.OpId
import org.apatheia.network.model.KadResponsePackage

trait ResponseStoreRef[F[_]] {
  def store(opId: OpId, response: KadResponsePackage): F[Unit]
  def get(opId: OpId): F[Option[KadResponsePackage]]
}
