package org.apatheia.network.client.response.store

import cats.effect.IO
import cats.effect.std.AtomicCell
import cats.effect.unsafe.implicits.global
import org.apatheia.network.model.{KadResponsePackage, OpId}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apatheia.store.KeyValueStore
import org.apatheia.store.ApatheiaKeyValueStore
import scala.collection.immutable.HashMap
import org.apatheia.model.NodeId
import java.util.UUID
import org.apatheia.network.model.KadCommand
import org.apatheia.network.model.KadResponseHeaders
import org.apatheia.network.model.KadResponsePayload

class DefaultResponseStoreRefSpec extends AnyFlatSpec with Matchers {

  "get()" should "return None if the opId is not in the store" in {
    val resultEffect = for {
      cell <- AtomicCell[IO].of[KeyValueStore[OpId, KadResponsePackage]](
        ApatheiaKeyValueStore[OpId, KadResponsePackage](HashMap.empty)
      )
      defaultResponseStoreRef = DefaultResponseStoreRef[IO](cell)
      response <- defaultResponseStoreRef.get(OpId(UUID.randomUUID()))
    } yield (response)

    val result: Option[KadResponsePackage] = resultEffect.unsafeRunSync()

    result shouldBe None
  }

  it should "return Some(response) if the opId is in the store" in new TestContext {
    val resultEffect = for {
      cell <- AtomicCell[IO].of[KeyValueStore[OpId, KadResponsePackage]](
        ApatheiaKeyValueStore[OpId, KadResponsePackage](
          HashMap(opId -> responsePackage)
        )
      )
      defaultResponseStoreRef = DefaultResponseStoreRef[IO](cell)
      response <- defaultResponseStoreRef.get(opId)
    } yield (response)

    val result: Option[KadResponsePackage] = resultEffect.unsafeRunSync()

    result shouldBe Some(responsePackage)
  }

  "store()" should "add the opId and response to the store" in new TestContext {
    val resultEffect = for {
      cell <- AtomicCell[IO].of[KeyValueStore[OpId, KadResponsePackage]](
        ApatheiaKeyValueStore[OpId, KadResponsePackage](HashMap.empty)
      )
      defaultResponseStoreRef = DefaultResponseStoreRef[IO](cell)
      responseBeforeStore <- defaultResponseStoreRef.get(opId)
      _ <- defaultResponseStoreRef.store(opId, responsePackage)
      responseAfterStore <- defaultResponseStoreRef.get(opId)
    } yield (responseBeforeStore, responseAfterStore)

    val result: (Option[KadResponsePackage], Option[KadResponsePackage]) =
      resultEffect.unsafeRunSync()

    result._1 shouldBe None
    result._2 shouldBe Some(responsePackage)
  }

  it should "overwrite the existing response if the opId is already in the store" in new TestContext {
    val resultEffect = for {
      cell <- AtomicCell[IO].of[KeyValueStore[OpId, KadResponsePackage]](
        ApatheiaKeyValueStore[OpId, KadResponsePackage](HashMap.empty)
      )
      defaultResponseStoreRef = DefaultResponseStoreRef[IO](cell)
      responseBeforeStore <- defaultResponseStoreRef.get(opId)
      _ <- defaultResponseStoreRef.store(opId, responsePackage)
      responseAfterStore <- defaultResponseStoreRef.get(opId)
      _ <- defaultResponseStoreRef.store(opId, updatedResponsePackage)
      responseAfterUpdate <- defaultResponseStoreRef.get(opId)
    } yield (responseBeforeStore, responseAfterStore, responseAfterUpdate)

    val result: (
        Option[KadResponsePackage],
        Option[KadResponsePackage],
        Option[KadResponsePackage]
    ) =
      resultEffect.unsafeRunSync()

    result._1 shouldBe None
    result._2 shouldBe Some(responsePackage)
    result._3 shouldBe Some(updatedResponsePackage)
  }

  trait TestContext {
    val opId = OpId(UUID.randomUUID())
    val responsePackage = KadResponsePackage(
      headers = KadResponseHeaders(
        from = NodeId(1),
        to = NodeId(2),
        opId = opId
      ),
      payload = KadResponsePayload(KadCommand.FindNode, Array.empty)
    )
    val updatedResponsePackage = responsePackage.copy(
      headers = responsePackage.headers.copy(opId = OpId(UUID.randomUUID()))
    )
  }

}
