package org.apatheia.network.client.impl

import cats.effect.kernel.Temporal
import org.apatheia.network.client.UDPClient
import java.net.InetSocketAddress
import org.apatheia.network.model.{ServerPort, UDPDatagram}
import org.apache.mina.core.service.IoConnector
import org.apache.mina.transport.socket.nio.NioDatagramConnector
import org.apache.mina.transport.socket.DatagramSessionConfig
import org.apache.mina.core.future.ConnectFuture
import org.apache.mina.core.buffer.IoBuffer
import java.nio.charset.StandardCharsets
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.implicits._
import org.apatheia.network.error.UDPClientError
import org.apatheia.network.model.MaxClientBufferSize
import scala.util.Try
import org.apatheia.network.model.MaxClientTimeout
import cats.effect.kernel.Async
import cats.effect.LiftIO
import org.apache.mina.core.future.CloseFuture

final case class DefaultUDPClient[F[_]: Async: LiftIO](
    maxBufferSize: MaxClientBufferSize,
    maxClientTimeout: MaxClientTimeout
) extends UDPClient[F] {

  private val connector: IoConnector = new NioDatagramConnector()
  connector.setHandler(UDPClientHandlerAdapter())
  private val sessionConfig =
    connector.getSessionConfig.asInstanceOf[DatagramSessionConfig]
  sessionConfig.setBroadcast(true)
  sessionConfig.setReuseAddress(true)
  sessionConfig.setWriteTimeout(maxClientTimeout.value)

  private val logger = Slf4jLogger.getLogger[F]

  private def writeBuffer(
      targetAddress: InetSocketAddress,
      future: ConnectFuture,
      udpData: Array[Byte]
  ): F[UDPClient.UDPSendResult] =
    if (udpData.size > maxBufferSize.value) {
      Async[F].pure(
        Either.left(UDPClientError.MaxSizeError(targetAddress))
      )
    } else if (future.isConnected()) {
      Async[F].delay {
        Try({
          val buffer: IoBuffer = IoBuffer.allocate(maxBufferSize.value)
          buffer.put(udpData)
          future.getSession.write(buffer)
          future.getSession.getCloseFuture.awaitUninterruptibly()
          Either.right((): Unit)
        }).toOption.getOrElse(
          Either.left(UDPClientError.WriteBufferError(targetAddress))
        )
      }
    } else {
      Async[F].pure(
        Either.left(UDPClientError.CannotConnectError(targetAddress))
      )
    }

  override def send(
      targetAddress: InetSocketAddress,
      data: Array[Byte]
  ): F[UDPClient.UDPSendResult] = {
    for {
      connectFuture <- Async[F].delay {
        connector.connect(targetAddress)
      }
      _ <- Async[F].delay { connectFuture.awaitUninterruptibly() }
      result <- writeBuffer(targetAddress, connectFuture, data)
      _ <- Async[F].delay { connector.dispose() }
    } yield (result)
  }

}
