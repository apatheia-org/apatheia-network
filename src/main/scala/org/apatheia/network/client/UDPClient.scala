package org.apatheia.network.client

import java.net.InetSocketAddress
import org.apatheia.network.error.UDPClientError

trait UDPClient[F[_]] {
  def send(
      targetAddress: InetSocketAddress,
      data: Array[Byte]
  ): F[UDPClient.UDPSendResult]
}

object UDPClient {
  type UDPSendResult = Either[UDPClientError, Unit]
}
