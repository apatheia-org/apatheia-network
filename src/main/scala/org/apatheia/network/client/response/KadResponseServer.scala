package org.apatheia.network.client.response

trait KadResponseServer[F[_]] {
  def run: F[Unit]
}
