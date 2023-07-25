package org.apatheia.network.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.util.UUID
import java.net.InetSocketAddress
import org.apatheia.model.NodeId
import org.apatheia.network.model.Codecs.NodeIdCodec._
import org.apatheia.network.model.Codecs.NodeIdCodec
import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure

class KadResponseHeadersSpec extends AnyFlatSpec with Matchers {

  "toObject()" should "return a KadHeaders object if data byte array is valid" in new TestContext {
    val validData =
      Array.concat(from.toByteArray, to.toByteArray, opId.toByteArray)
    val test1 = validData.drop(NodeIdCodec.BYTESIZE).take(NodeIdCodec.BYTESIZE)
    val result = validData.toObject[KadResponseHeaders]

    result shouldBe Right(KadResponseHeaders(from, to, opId))
  }

  it should "return DecodingFailure object if data byte array is invalid" in {
    val invalidData = "A".getBytes()
    val result = invalidData.toObject[KadResponseHeaders]

    result shouldBe Left(
      DecodingFailure(
        "Unexpected error while parsing a byte array into a NodeId: java.lang.NumberFormatException: For input string: \"A\""
      )
    )
  }

  trait TestContext {
    val opId = OpId(UUID.randomUUID())
    val baseDatagram = UDPDatagram(
      from = new InetSocketAddress("127.0.0.1", 8888),
      to = new InetSocketAddress("127.0.0.1", 9999),
      data = Array.fill[Byte](16)(0)
    )
    val from = NodeId(1)
    val to = NodeId(2)
    val headers = KadResponseHeaders(
      from = from,
      to = to,
      opId = opId
    )

  }

}
