package org.apatheia.network.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apatheia.model.NodeId
import java.net.InetSocketAddress
import java.util.UUID
import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure

class KadResponsePackageSpec extends AnyFlatSpec with Matchers {

  "toObject()" should "return KadResponsePackage if datagram is able to be parsed into response" in new TestContext {
    validDatagram.data.toObject[KadResponsePackage].isRight shouldBe true
  }

  "toObject()" should "return PackageDataParsingError if datagram is not able to be parsed into response" in new TestContext {
    invalidDatagram.data.toObject[KadResponsePackage].isLeft shouldBe true
  }

  trait TestContext {
    val opId = OpId(UUID.randomUUID())
    val baseDatagram = UDPDatagram(
      from = new InetSocketAddress("127.0.0.1", 8888),
      to = new InetSocketAddress("127.0.0.1", 9999),
      data = Array.fill[Byte](16)(0)
    )
    val basePayload =
      KadResponsePayload(KadCommand.FindNode, Array.fill[Byte](16)(0))
    val responsePackage = KadResponsePackage(
      headers = KadResponseHeaders(
        from = NodeId(1),
        to = NodeId(2),
        opId = opId
      ),
      payload = KadResponsePayload(KadCommand.FindNode, Array.fill[Byte](16)(0))
    )
    val validDatagram =
      baseDatagram.copy(data = responsePackage.toByteArray)
    val invalidDatagram = baseDatagram.copy(data = "A".getBytes())
  }

}
