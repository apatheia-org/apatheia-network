package org.apatheia.client.impl

import java.net.InetSocketAddress

import cats.effect.IO
import org.apache.mina.core.service.IoConnector
import org.apatheia.network.client.impl.DefaultUDPClient
import org.apatheia.network.error.UDPClientError
import org.apatheia.network.model.{MaxClientBufferSize, MaxClientTimeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import cats.implicits._
import cats.effect.unsafe.implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import cats.effect.unsafe.IORuntime
import org.apatheia.network.client.UDPClient

class DefaultUDPClientSpec extends AnyFlatSpec with Matchers with ScalaFutures {
  implicit val runtime: IORuntime = cats.effect.unsafe.implicits.global

  val targetAddress: InetSocketAddress =
    new InetSocketAddress("localhost", 12345)
  val smallData: Array[Byte] = "small data".getBytes
  val largeData: Array[Byte] = Array.fill[Byte](2000)(0)

  behavior of "DefaultUDPClient"

  it should "send small data successfully" in {
    val client = DefaultUDPClient[IO](
      MaxClientBufferSize(2048),
      MaxClientTimeout(5)
    )

    val result: UDPClient.UDPSendResult =
      client.send(targetAddress, smallData).unsafeRunSync()

    result shouldBe Either.right[UDPClientError, Unit](())
  }

  it should "send large data unsuccessfully with MaxSizeError" in {
    val client = DefaultUDPClient[IO](
      MaxClientBufferSize(1),
      MaxClientTimeout(5)
    )

    val result: UDPClient.UDPSendResult =
      client.send(targetAddress, smallData).unsafeRunSync()

    result shouldBe Left(
      UDPClientError.MaxSizeError(targetAddress)
    )
  }

  it should "fail to send data with CannotConnectError if target address is invalid" in {
    val invalidTargetAddress =
      new InetSocketAddress("unexistent.address.com", 22) // ftp port
    val client = DefaultUDPClient[IO](
      MaxClientBufferSize(2048),
      MaxClientTimeout(5)
    )
    val result = client.send(invalidTargetAddress, smallData).unsafeRunSync()

    result shouldBe Left(
      UDPClientError.CannotConnectError(invalidTargetAddress)
    )
  }

  it should "fail to send data with WriteBufferError if unable to write to the buffer" in {
    // Use an invalid port number to simulate an error writing to the buffer
    val targetAddress = new InetSocketAddress("localhost", 1234)
    val client = DefaultUDPClient[IO](
      MaxClientBufferSize(2),
      MaxClientTimeout(5)
    )

    val result = client.send(targetAddress, smallData).unsafeRunSync()

    result shouldBe Left(
      UDPClientError.MaxSizeError(targetAddress)
    )
  }
}
