package org.apatheia.network.server.impl

import org.apatheia.network.model.ServerPort
import cats.effect.kernel.Async
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor
import org.apache.mina.transport.socket.DatagramSessionConfig
import java.net.InetSocketAddress
import cats.implicits._
import cats.effect.std.Dispatcher
import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import org.apache.mina.core.buffer.IoBuffer
import org.apatheia.network.model.UDPDatagram
import org.apatheia.network.server.UDPServer
import org.apatheia.network.server.UDPDatagramReceiver

final case class DefaultUDPServer[F[_]: Async](
    serverPort: ServerPort,
    receiver: UDPDatagramReceiver[F]
)(implicit dispatcher: Dispatcher[F])
    extends UDPServer[F] {

  private val acceptor = new NioDatagramAcceptor()

  override def run(): F[Unit] =
    for {
      handler <- Async[F].delay(
        UDPServerHandlerAdapter(receiver)
      )
      config <- Async[F].delay {
        val config =
          acceptor.getSessionConfig.asInstanceOf[DatagramSessionConfig]
        config.setReuseAddress(true)
        config.setBroadcast(true)
        config.setReceiveBufferSize(65536)
        config
      }
      _ <- Async[F].delay {
        acceptor.setHandler(handler)
        acceptor.bind(new InetSocketAddress(serverPort.value))
      }
    } yield ()

  override def stop(): F[Unit] = Async[F].delay {
    acceptor.dispose()
  }

}
