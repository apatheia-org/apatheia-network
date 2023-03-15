package org.apatheia.network.meta

trait LocalhostMetadataRef[F[_]] {
  def get: F[LocalhostMetadata]
  def modify(localhostMetadata: LocalhostMetadata): F[Unit]
}
