package org.apatheia.network.model

final case class KadResponsePackage(
    headers: KadHeaders,
    udpDatagram: UDPDatagram
)
