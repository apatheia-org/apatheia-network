package org.apatheia.network.model

final case class KadResponsePackage(
    headers: KadHeaders,
    payload: Option[KadResponsePayload],
    udpDatagram: UDPDatagram
)
