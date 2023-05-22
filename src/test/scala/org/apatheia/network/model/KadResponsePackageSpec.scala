package org.apatheia.network.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apatheia.model.NodeId
import java.net.InetSocketAddress
import java.util.UUID

class KadResponsePackageSpec extends AnyFlatSpec with Matchers {

  "parse()" should "return KadResponsePackage if datagram is able to be parsed into response" in new TestContext {
    KadResponsePackage.parse(validDatagram).isRight shouldBe true
  }

  "parse()" should "return PackageDataParsingError if datagram is not able to be parsed into response" in new TestContext {
    KadResponsePackage.parse(invalidDatagram).isLeft shouldBe true
  }

  trait TestContext {
    val opId = OpId(UUID.randomUUID())
    val baseDatagram = UDPDatagram(
      from = new InetSocketAddress("127.0.0.1", 8888),
      to = new InetSocketAddress("127.0.0.1", 9999),
      data = Array.fill[Byte](16)(0)
    )
    val baseResponsePackage = KadResponsePackage(
      headers = KadHeaders(
        from = NodeId(1),
        to = NodeId(2),
        opId = opId
      ),
      payload =
        KadResponsePayload(KadCommand.FindNode, Array.fill[Byte](16)(0)),
      udpDatagram = baseDatagram
    )
    val validDatagram =
      baseDatagram.copy(data = baseResponsePackage.toByteArray)
    val responsePackage = baseResponsePackage.copy(udpDatagram = validDatagram)
    val invalidDatagram = baseDatagram.copy(data = "A".getBytes())
  }

}
