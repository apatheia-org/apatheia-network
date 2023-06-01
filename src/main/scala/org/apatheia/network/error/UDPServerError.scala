package org.apatheia.network.error

import org.apatheia.error.PackageDataParsingError

case class UDPServerError(message: String) extends Error

object UDPServerError {
  object ExtractAddressError
      extends UDPServerError(
        "Error to extract address from incoming UDP session"
      )

  object ExtractBufferError
      extends UDPServerError(
        "Error to extract buffer from incoming UDP message"
      )

  class DatagramParsingError(
      packageDataParsingError: PackageDataParsingError
  ) extends UDPServerError(
        "Error to extract buffer from incoming UDP message"
      )

}
