package org.apatheia.server.impl

import org.apatheia.network.server._
import org.apatheia.network.server.impl._
import java.net.InetSocketAddress
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.apatheia.network.model.ServerPort
import org.scalatest.flatspec.AnyFlatSpec
import org.apatheia.network.model.UDPDatagram
import org.scalatest.matchers.should.Matchers
import org.apatheia.network.client.impl.DefaultUDPClient
import org.apatheia.network.model.MaxClientBufferSize
import org.apatheia.network.model.MaxClientTimeout
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.std.Dispatcher
import cats.syntax.distributive
import cats.effect.unsafe.IORuntime

class DefaultUDPServerSpec extends AnyFlatSpec with Matchers {

  val serverPort = ServerPort(9999)
  val logger = Slf4jLogger.getLoggerFromClass[IO](this.getClass())
  // implicit val asyncIO = IO.asyncForIO

  "DefaultUDPServer" should "start and stop successfully" in {
    val receiver = new UDPDatagramReceiver[IO] {
      override def onUDPDatagramReceived(datagram: UDPDatagram): IO[Unit] =
        IO.unit
    }

    val effect = Dispatcher[IO].use(implicit dispatcher => {

      val server =
        DefaultUDPServer[IO](serverPort, receiver)

      for {
        _ <- server.run().start // Start the server in a separate fiber
        _ <- IO(Thread.sleep(100)) // Wait for the server to start up
        _ <- server.stop() // Stop the server
      } yield succeed
    })
  }

  it should "receive and handle a UDP datagram successfully" in {
    val expectedDatagram = UDPDatagram(
      from = new InetSocketAddress("127.0.0.1", 8888),
      to = new InetSocketAddress("127.0.0.1", serverPort.value),
      data = Array[Byte](1, 2, 3, 4)
    )

    val receiver = new UDPDatagramReceiver[IO] {
      override def onUDPDatagramReceived(datagram: UDPDatagram): IO[Unit] = {
        logger
          .debug("Processing datagram...")
      }
    }

    val client = DefaultUDPClient[IO](
      maxBufferSize = MaxClientBufferSize(100000),
      maxClientTimeout = MaxClientTimeout(4)
    )

    val effect = Dispatcher[IO].use(implicit dispatcher => {
      val server =
        DefaultUDPServer[IO](serverPort, receiver)

      for {
        _ <- server.run()
        _ <- IO(Thread.sleep(100)) // Wait for the server to start up
        _ <- client.send(expectedDatagram.to, expectedDatagram.data)
        _ <- IO(
          Thread.sleep(1000)
        ) // Wait for the datagram to be received and handled
        _ <- server.stop() // Stop the server
      } yield succeed
    })

    effect.unsafeRunSync()
  }

}
