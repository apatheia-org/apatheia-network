package org.apatheia.network.client.response.consumer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apatheia.network.client.response.store.ResponseStoreRef
import cats.effect.IO
import org.apatheia.network.model.{KadResponsePackage, OpId}
import org.apatheia.network.model.KadHeaders
import org.apatheia.model.NodeId
import org.apatheia.network.model.KadResponsePayload
import org.apatheia.network.model.KadCommand
import java.util.UUID
import org.apatheia.network.model.UDPDatagram
import java.net.InetSocketAddress
import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class KadResponseConsumerSpec extends AnyFlatSpec with Matchers {

  "consumeResponse()" should "return response for an OpId" in new TestContext {
    val responseConsumer = DefaultKadResponseConsumer(nonEmptyResponseStoreRef)

    val result = responseConsumer.consumeResponse(opId, timeout).unsafeRunSync()

    result shouldBe Some(responsePackage)
  }

  "consumeResponse()" should "not return response if timeout is breached" in new TestContext {
    val responseConsumer = DefaultKadResponseConsumer(emptyResponseStoreRef)

    val timeBeforeConsumeResponse = LocalDateTime.now()
    val result = responseConsumer.consumeResponse(opId, timeout).unsafeRunSync()
    val timeAfterConsumerResponse = LocalDateTime.now()
    val ellapsedTime = ChronoUnit.SECONDS.between(
      timeBeforeConsumeResponse,
      timeAfterConsumerResponse
    )

    result shouldBe None
    ellapsedTime should be > 10L
  }

  trait TestContext {
    val timeout = 10.seconds

    val opId = OpId(UUID.randomUUID())

    val datagram = UDPDatagram(
      from = new InetSocketAddress("127.0.0.1", 8888),
      to = new InetSocketAddress("127.0.0.1", 9999),
      data = Array.empty
    )

    val responsePackage = KadResponsePackage(
      headers = KadHeaders(
        from = NodeId(1),
        to = NodeId(2),
        opId = opId
      ),
      payload = KadResponsePayload(KadCommand.FindNode, Array.empty),
      udpDatagram = datagram
    )

    val nonEmptyResponseStoreRef: ResponseStoreRef[IO] =
      new ResponseStoreRef[IO] {
        override def store(opId: OpId, response: KadResponsePackage): IO[Unit] =
          ???
        override def get(opId: OpId): IO[Option[KadResponsePackage]] =
          IO.pure(Some(responsePackage))
      }

    val emptyResponseStoreRef: ResponseStoreRef[IO] =
      new ResponseStoreRef[IO] {
        override def store(opId: OpId, response: KadResponsePackage): IO[Unit] =
          ???
        override def get(opId: OpId): IO[Option[KadResponsePackage]] =
          IO.pure(None)
      }

  }

}
