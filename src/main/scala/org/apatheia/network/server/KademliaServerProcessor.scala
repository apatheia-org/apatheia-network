package org.apatheia.network.server

import org.apatheia.algorithm.findnode.FindNodeClient
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.UDPDatagram

trait KademliaServerProcessor[F[_]] {

  def process(
      kadDatagram: KadDatagramPackage,
      udpDatagram: UDPDatagram
  ): F[Unit]

}
