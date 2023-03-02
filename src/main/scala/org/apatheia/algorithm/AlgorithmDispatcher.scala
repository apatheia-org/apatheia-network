package org.apatheia.algorithm

import org.apatheia.network.model.KadDatagramPackage

trait AlgorithmDispatcher[F[_]] {
  def dispatch(datagramPackage: KadDatagramPackage): F[Unit]
}
