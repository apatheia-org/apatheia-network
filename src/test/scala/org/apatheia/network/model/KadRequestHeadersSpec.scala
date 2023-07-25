package org.apatheia.network.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.util.UUID
import java.net.InetSocketAddress
import org.apatheia.model.NodeId
import org.apatheia.codec.Codec._
import org.apatheia.network.model.Codecs.NodeIdCodec._
import org.apatheia.codec.DecodingFailure

class KadRequestHeadersSpec extends AnyFlatSpec with Matchers {

  "parse()" should "return a KadRequestHeaders object if data byte array is valid" in new TestContext {
    val validData =
      Array.concat(
        from.toByteArray,
        to.toByteArray,
        opId.toByteArray,
        serverPort.toByteArray
      )
    val result = validData.toObject[KadRequestHeaders]

    result shouldBe Right(KadRequestHeaders(from, to, opId, serverPort))
  }

  it should "return PackageDataParsingError object if data byte array is invalid" in {
    val invalidData = "A".getBytes()
    val result = invalidData.toObject[KadRequestHeaders]

    result shouldBe Left(
      DecodingFailure(
        "Unexpected error while parsing a byte array into a NodeId: java.lang.NumberFormatException: For input string: \"A\""
      )
    )
  }

  trait TestContext {
    val opId = OpId(UUID.randomUUID())
    val serverPort = ServerPort(9999)
    val baseDatagram = UDPDatagram(
      from = new InetSocketAddress("127.0.0.1", 8888),
      to = new InetSocketAddress("127.0.0.1", 9999),
      data = Array.fill[Byte](16)(0)
    )
    val from = NodeId(1)
    val to = NodeId(2)
    val headers = KadRequestHeaders(
      from = from,
      to = to,
      opId = opId,
      responseServerPort = serverPort
    )

  }

}
