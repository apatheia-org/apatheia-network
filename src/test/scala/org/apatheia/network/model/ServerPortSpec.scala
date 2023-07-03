package org.apatheia.network.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.apatheia.network.model.ServerPort
import org.apatheia.error.PackageDataParsingError
import java.nio.ByteBuffer

class ServerPortSpec extends AnyWordSpec with Matchers {

  "ServerPort" should {

    "be parsed correctly from a byte array" in {
      val portValue: Int = 8080
      val byteArray: Array[Byte] =
        ByteBuffer.allocate(ServerPort.byteSize).putInt(portValue).array()

      val result = ServerPort.parse(byteArray)

      result shouldBe Right(ServerPort(portValue))
    }

    "return an error when parsing invalid data" in {
      val invalidByteArray: Array[Byte] =
        Array(0, 0, 0) // Invalid byte array length

      val result = ServerPort.parse(invalidByteArray)

      result shouldBe Left(
        PackageDataParsingError("Error while parsing ServerPort corrupt data")
      )
    }

    "convert ServerPort to a byte array" in {
      val portValue: Int = 1234
      val serverPort = ServerPort(portValue)
      val expectedByteArray: Array[Byte] =
        Array(0, 0, 4, -46) // Equivalent to 1234 in little-endian byte order

      val byteArray = serverPort.toByteArray

      byteArray shouldBe expectedByteArray
    }

  }

}
