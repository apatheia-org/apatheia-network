package org.apatheia.network.model

case class KadDatagramPackage(
    headers: KadHeaders,
    payload: KadDatagramPayload,
    udpDatagram: UDPDatagram
)
