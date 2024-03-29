package org.apatheia.network.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.apatheia.codec.Codec._
import org.apatheia.codec.DecodingFailure
import org.apatheia.network.model.KadCommand._

class KadCommandSpec extends AnyWordSpec with Matchers {

  "KadCommand" should {

    "have correct byte value" in {
      KadCommand.FindNode.value shouldBe 0
      KadCommand.FindValue.value shouldBe 1
      KadCommand.Store.value shouldBe 2
    }

    "be able to convert to byte array" in {
      val findNode: KadCommand = KadCommand.FindNode
      val findValue: KadCommand = KadCommand.FindValue
      val store: KadCommand = KadCommand.Store

      val findNodeByteArray: Array[Byte] = findNode.toByteArray
      val findValueByteArray = findValue.toByteArray
      val storeByteArray = store.toByteArray

      // Perform assertions on the byte arrays
      // Example assertion: findNodeByteArray should have length 1 and its value should be 0
      findNodeByteArray.length shouldBe 1
      findNodeByteArray(0) shouldBe 0

      // Example assertion: findValueByteArray should have length 1 and its value should be 1
      findValueByteArray.length shouldBe 1
      findValueByteArray(0) shouldBe 1

      // Example assertion: storeByteArray should have length 1 and its value should be 2
      storeByteArray.length shouldBe 1
      storeByteArray(0) shouldBe 2
    }

    "be able to parse a byte array into KadCommand" in {
      val findNodeByteArray = Array[Byte](0)
      val findValueByteArray = Array[Byte](1)
      val storeByteArray = Array[Byte](2)
      val invalidByteArray = Array[Byte](3)

      val parsedFindNode = findNodeByteArray.toObject[KadCommand]
      val parsedFindValue = findValueByteArray.toObject[KadCommand]
      val parsedStore = storeByteArray.toObject[KadCommand]
      val parsedInvalid = invalidByteArray.toObject[KadCommand]

      // Perform assertions on the parsed results
      // Example assertion: parsedFindNode should be a Right containing KadCommand.FindNode
      parsedFindNode shouldBe Right(KadCommand.FindNode)

      // Example assertion: parsedFindValue should be a Right containing KadCommand.FindValue
      parsedFindValue shouldBe Right(KadCommand.FindValue)

      // Example assertion: parsedStore should be a Right containing KadCommand.Store
      parsedStore shouldBe Right(KadCommand.Store)

      // Example assertion: parsedInvalid should be a Left containing a DecodingFailure
      parsedInvalid shouldBe a[Left[_, _]]
      parsedInvalid.left.get shouldBe a[DecodingFailure]
    }

  }

}
