package org.apatheia.network.client.response.store

import cats.effect.kernel.Async
import org.apatheia.network.model.{KadResponsePackage, OpId}
import org.apatheia.store.ApatheiaKeyValueStore
import scala.collection.immutable.HashMap
import cats.effect.std.AtomicCell
import cats.effect.kernel.Sync
import cats.implicits._
import org.apatheia.store.KeyValueStore

case class DefaultResponseStoreRef[F[_]: Async](
    cell: AtomicCell[F, KeyValueStore[OpId, KadResponsePackage]]
) extends ResponseStoreRef[F] {

  override def get(opId: OpId): F[Option[KadResponsePackage]] =
    cell.get.flatMap(map => Async[F].pure(map.get(opId)))

  override def store(opId: OpId, response: KadResponsePackage): F[Unit] =
    cell.modify(store => (store.put(opId, response), store))

}
