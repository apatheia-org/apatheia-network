package org.apatheia.network.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.apatheia.error.PackageDataParsingError
import org.apatheia.model.PackageDataParser

class KadResponsePayloadSpec extends AnyWordSpec with Matchers {

  "KadResponsePayload" should {

    val command = KadCommand.FindNode
    val data = Array[Byte](1, 2, 3)
    val payload = KadResponsePayload(command, data)

    "be able to convert to byte array" in {
      val byteArray = payload.toByteArray

      // Perform assertions on the byteArray
      // Example assertion: byteArray should have the correct length
      byteArray.length shouldBe data.length + 1
    }

    "be able to parse a byte array into KadResponsePayload" in {
      val byteArray = Array.concat(Array(command.value.toByte), data)

      val parsedResult = KadResponsePayload.parse(byteArray)

      // Perform assertions on the parsedResult
      // Example assertion: parsedResult should be a Right containing a KadResponsePayload
      parsedResult.isRight shouldBe true
      parsedResult.right.get.command shouldBe command
      parsedResult.right.get.data shouldBe data
    }

    "return a parsing error when unable to parse a byte array into KadResponsePayload" in {
      val invalidByteArray =
        Array[Byte](10, 2, 3) // Invalid byte array with incorrect length

      val parsedResult = KadResponsePayload.parse(invalidByteArray)

      // Perform assertions on the parsedResult
      // Example assertion: parsedResult should be a Left containing a PackageDataParsingError
      parsedResult.isLeft shouldBe true
      parsedResult.left.get shouldBe a[PackageDataParsingError]
    }

  }

}
