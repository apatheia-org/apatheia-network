package org.apatheia.network

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import org.apatheia.network.client.impl.DefaultUDPClient
import org.apatheia.network.model.MaxClientTimeout
import org.apatheia.network.model.MaxClientBufferSize
import java.net.InetSocketAddress
import java.net.InetAddress

object ShittyUDPClient extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val client: DefaultUDPClient[IO] = DefaultUDPClient[IO](
      maxBufferSize = MaxClientBufferSize(1),
      maxClientTimeout = MaxClientTimeout(10000)
    )

    val host = InetAddress.getByName("100.100.11.1")
    val target = new InetSocketAddress(host, 3333)
    client
      .send(target, Array(1.toByte, 2.toByte))
      .flatMap(_ => IO(ExitCode.Success))
  }
}
