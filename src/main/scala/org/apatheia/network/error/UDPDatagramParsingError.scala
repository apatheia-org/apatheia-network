package org.apatheia.network.error

import org.apatheia.error.Error

final case class UDPDatagramParsingError(message: String) extends Error
