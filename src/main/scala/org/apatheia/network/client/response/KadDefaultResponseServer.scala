package org.apatheia.network.client.response

import cats.effect.kernel.Async

final case class KadDefaultResponseServer[F[_]: Async]()
    extends KadResponseServer[F] {
  override def run: F[Unit] = ???
}
