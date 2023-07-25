package org.apatheia.network.meta

import cats.effect.std.AtomicCell
import cats.effect.kernel.Async
import org.apatheia.network.model.LocalhostMetadata
import cats.implicits._
import org.apatheia.model.Contact

case class DefaultLocalhostMetadataRef[F[_]: Async](
    cell: AtomicCell[F, LocalhostMetadata]
) extends LocalhostMetadataRef[F] {

  override def get: F[LocalhostMetadata] = cell.get
  override def modify(newLocalhostMetadata: LocalhostMetadata): F[Unit] =
    cell.update(_ => newLocalhostMetadata).flatMap(_ => Async[F].unit)

  override def updateRoutingTable(contacts: Set[Contact]): F[Unit] = for {
    localhostMetadata <- get
    routingTable = localhostMetadata.routingTable
    _ <- modify(
      localhostMetadata.copy(routingTable = routingTable.addContacts(contacts))
    )
  } yield ()

}
