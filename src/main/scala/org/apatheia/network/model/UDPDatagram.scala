package org.apatheia.network.model

import java.net.InetSocketAddress

final case class UDPDatagram(
    from: InetSocketAddress,
    to: InetSocketAddress,
    data: Array[Byte] = Array.empty
) {
  override def toString = s"UDPDatagram(${data.mkString})"
}
