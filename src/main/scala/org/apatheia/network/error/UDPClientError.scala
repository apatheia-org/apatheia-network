package org.apatheia.network.error

import java.net.InetSocketAddress

class UDPClientError(message: String) extends Error

object UDPClientError {
  case class CannotConnectError(targetAddress: InetSocketAddress)
      extends UDPClientError(
        s"Cannot Connect to ${targetAddress.getHostName()}:${targetAddress.getPort()}"
      )

  case class MaxSizeError(targetAddress: InetSocketAddress)
      extends UDPClientError(
        s"Data payload has bytesize bigger than max allowed for the package sent to to ${targetAddress
            .getHostName()}:${targetAddress.getPort()}"
      )

  case class WriteBufferError(targetAddress: InetSocketAddress)
      extends UDPClientError(
        s"Error while writing the UDP package buffer to to ${targetAddress
            .getHostName()}:${targetAddress.getPort()}"
      )

}
