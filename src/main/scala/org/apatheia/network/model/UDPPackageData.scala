package org.apatheia.network.model

import org.apatheia.model.PackageData

trait UDPPackageData extends PackageData {
  def udpDatagram: UDPDatagram
}
