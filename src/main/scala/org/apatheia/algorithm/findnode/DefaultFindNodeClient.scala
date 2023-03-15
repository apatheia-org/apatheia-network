package org.apatheia.algorithm.findnode

import cats.effect.kernel.Async
import org.apatheia.model.Contact
import org.apatheia.network.client.response.consumer.KadResponseConsumer
import org.apatheia.network.model.OpId
import scala.concurrent.duration.Duration
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.client.UDPClient
import org.apatheia.network.model.KadHeaders
import org.apatheia.network.meta.DefaultLocalhostMetadataRef
import org.apatheia.network.model.KadDatagramPackage
import org.apatheia.network.model.KadDatagramPayload
import org.apatheia.network.model.KadCommand
import org.apatheia.model.NodeId
import cats.implicits._
import java.net.InetSocketAddress

final case class DefaultFindNodeClient[F[_]: Async](
    kadResponseConsumer: KadResponseConsumer[F],
    responseTimeout: Duration,
    udpClient: UDPClient[F],
    defaultLocalhostMetadataRef: DefaultLocalhostMetadataRef[F]
) extends FindNodeClient[F] {

  private def toKadDatagramPackage(
      remote: Contact,
      target: NodeId
  ): F[KadDatagramPackage] =
    defaultLocalhostMetadataRef.get.flatMap(locahostMetadata =>
      Async[F].pure {
        KadDatagramPackage(
          headers = KadHeaders(
            from = locahostMetadata.localContact.nodeId,
            to = remote.nodeId,
            opId = OpId.random
          ),
          payload = KadDatagramPayload(
            command = KadCommand.FindNode,
            data = target.toByteArray
          )
        )
      }
    )

  private def toContacts(
      responsePackage: Option[KadResponsePackage]
  ): F[List[Contact]] = ???
  // responsePackage.map(kadDatagram => kadDatagram.payload.)

  override def requestContacts(
      remote: Contact,
      target: NodeId
  ): F[List[Contact]] = for {
    kadDatagramPackage <- toKadDatagramPackage(remote, target)
    _ <- udpClient.send(
      new InetSocketAddress(remote.ip, remote.port),
      kadDatagramPackage.toByteArray
    )
    response <- kadResponseConsumer.consumeResponse(
      opId = kadDatagramPackage.headers.opId,
      timeout = responseTimeout
    )
    contacts <- toContacts(response)
  } yield (contacts)

  // use network information contained in Contact object will be used by UDP client
  // send request to the target
  // once request is sent, initialize retry on ResponseStore through response consumer
  // if response is not empty parse the byte array received from ext. server into a List of contacts
  // if response is None return an empty list

  // this is real software engineering
  // not bureacractic and dogmatic playshit: GOF Design patterns, SOLID, Hexagonal arch(it's not hexagonal btw)
  // time 4 a fucking smoke

}
