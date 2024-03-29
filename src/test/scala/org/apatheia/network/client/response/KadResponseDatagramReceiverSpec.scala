package org.apatheia.network.client.response

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apatheia.network.model.OpId
import java.util.UUID
import org.apatheia.network.model.UDPDatagram
import org.apatheia.network.model.KadResponsePackage
import java.net.InetSocketAddress
import org.apatheia.model.NodeId
import cats.effect.IO
import org.apatheia.network.client.response.store.DefaultResponseStoreRef
import org.apatheia.store.KeyValueStore
import cats.effect.std.AtomicCell
import org.apatheia.store.ApatheiaKeyValueStore
import scala.collection.immutable

import cats.effect.unsafe.implicits.global
import org.scalatest.OptionValues
import org.apatheia.network.model.KadResponseHeaders
import org.apatheia.network.model.KadResponsePayload
import org.apatheia.network.model.KadCommand
import org.apatheia.codec.Codec._

class KadResponseDatagramReceiverSpec
    extends AnyFlatSpec
    with Matchers
    with OptionValues {

  "onUDPDatagramReceived()" should "should store received datagram if data is able to be parsed" in new TestContext {

    val resultEffect = for {
      cell <- AtomicCell[IO].of[KeyValueStore[OpId, KadResponsePackage]](
        ApatheiaKeyValueStore[OpId, KadResponsePackage](
          immutable.HashMap.empty
        )
      )
      defaultResponseStoreRef = DefaultResponseStoreRef[IO](cell)
      datagramReceiver = KadResponseDatagramReceiver[IO](
        defaultResponseStoreRef
      )
      _ <- datagramReceiver.onUDPDatagramReceived(datagram)
      storedDatagram <- defaultResponseStoreRef.get(opId)
    } yield (storedDatagram)

    val result = resultEffect.unsafeRunSync()

    result.value.headers shouldBe responsePackage.headers
    result.value.toByteArray shouldBe responsePackage.toByteArray
  }

  trait TestContext {
    val opId = OpId(UUID.randomUUID())
    val data = Array.fill[Byte](16)(0)
    val responsePackage = KadResponsePackage(
      headers = KadResponseHeaders(
        from = NodeId(1),
        to = NodeId(2),
        opId = opId
      ),
      payload = KadResponsePayload(
        command = KadCommand.FindNode,
        data = data
      )
    )

    val datagram = UDPDatagram(
      from = new InetSocketAddress("127.0.0.1", 8888),
      to = new InetSocketAddress("127.0.0.1", 9999),
      data = responsePackage.toByteArray
    )

  }

}
