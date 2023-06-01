package org.apatheia.network.model

import org.apatheia.model.Contact
import org.apatheia.model.RoutingTable
import java.net.InetSocketAddress

case class LocalhostMetadata(
    localContact: Contact,
    routingTable: RoutingTable
) {
  final val from: InetSocketAddress =
    new InetSocketAddress(localContact.ip, localContact.port)
}
