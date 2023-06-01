package org.apatheia.network.meta

import cats.effect.std.AtomicCell
import cats.effect.kernel.Async
import org.apatheia.network.model.OpId
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.model.LocalhostMetadata
import cats.syntax.flatMap._

case class DefaultLocalhostMetadataRef[F[_]: Async](
    cell: AtomicCell[F, LocalhostMetadata]
) extends LocalhostMetadataRef[F] {

  override def get: F[LocalhostMetadata] = cell.get
  override def modify(newLocalhostMetadata: LocalhostMetadata): F[Unit] =
    cell.update(_ => newLocalhostMetadata).flatMap(_ => Async[F].unit)

}
