package org.apatheia.network.server

trait UDPServer[F[_]] {
  def run(): F[Unit]
  def stop(): F[Unit]
}
