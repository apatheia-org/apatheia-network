package org.apatheia.network.model

import java.util.UUID
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import java.nio.ByteBuffer

class OpIdSpec extends AnyFlatSpec with Matchers with EitherValues {

  "parse" should "return a valid OpId from a 16-byte array" in {
    val uuid = UUID.randomUUID()
    val byteArray = ByteBuffer
      .wrap(new Array[Byte](16))
      .putLong(uuid.getMostSignificantBits())
      .putLong(uuid.getLeastSignificantBits())
      .array()
    val opId = OpId.parse(byteArray)
    opId.isRight shouldBe true
    opId.value.value shouldBe uuid
  }

  it should "return a PackageDataParsingError if the array is not 16 bytes long" in {
    val byteArray = Array[Byte](1, 2, 3, 4)
    val opId = OpId.parse(byteArray)
    opId.isLeft shouldBe true
    opId.left.value.message shouldBe "Error while parsing OpId corrupt data"
  }

}
