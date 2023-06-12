package org.apatheia.network.client.impl

import org.apatheia.network.client.UDPClient
import java.net.InetSocketAddress
import org.apache.mina.core.service.IoConnector
import org.apache.mina.transport.socket.nio.NioDatagramConnector
import org.apache.mina.transport.socket.DatagramSessionConfig
import org.apache.mina.core.future.ConnectFuture
import org.apache.mina.core.buffer.IoBuffer
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.implicits._
import org.apatheia.network.error.UDPClientError
import org.apatheia.network.model.MaxClientBufferSize
import scala.util.Try
import org.apatheia.network.model.MaxClientTimeout
import cats.effect.kernel.Async
// import cats.effect.LiftIO

final case class DefaultUDPClient[F[_]: Async](
    maxBufferSize: MaxClientBufferSize,
    maxClientTimeout: MaxClientTimeout
) extends UDPClient[F] {

  private val logger = Slf4jLogger.getLogger[F]

  private def writeBuffer(
      targetAddress: InetSocketAddress,
      future: ConnectFuture,
      udpData: Array[Byte]
  ): F[UDPClient.UDPSendResult] =
    if (udpData.size > maxBufferSize.value) {
      logger.debug(
        s"Udp data size: ${udpData.size}. Max buffer size: ${maxBufferSize}"
      ) *>
        Async[F].pure(
          Either.left(UDPClientError.MaxSizeError(targetAddress))
        )
    } else if (future.isConnected()) {
      Async[F].delay {
        Try({
          val buffer: IoBuffer = IoBuffer.allocate(maxBufferSize.value)
          buffer.put(udpData)
          buffer.flip()
          future.getSession.write(buffer)
          Either.right((): Unit)
        }).toOption.getOrElse(
          Either.left(UDPClientError.WriteBufferError(targetAddress))
        )
      }
    } else {
      logger.error(
        s"Error while writing message: ${UDPClientError.CannotConnectError(targetAddress)}"
      ) *>
        Async[F].pure(
          Either.left(UDPClientError.CannotConnectError(targetAddress))
        )
    }

  private def buildClientConnection(
      targetAddress: InetSocketAddress
  ): F[IoConnector] = Async[F].delay {
    val connector: IoConnector = new NioDatagramConnector()
    connector.setDefaultRemoteAddress(targetAddress)
    connector.setHandler(UDPClientHandlerAdapter())
    val sessionConfig =
      connector.getSessionConfig.asInstanceOf[DatagramSessionConfig]
    sessionConfig.setBroadcast(true)
    sessionConfig.setReuseAddress(true)
    sessionConfig.setWriteTimeout(maxClientTimeout.value)
    connector
  }

  override def send(
      targetAddress: InetSocketAddress,
      data: Array[Byte]
  ): F[UDPClient.UDPSendResult] = {
    for {
      _ <- logger.debug(s"Sending UDP request to ${targetAddress}")
      connector <- buildClientConnection(targetAddress)
      connectFuture <- Async[F].delay {
        connector.connect()
      }
      _ <- logger.debug(s"Await for connection to ${targetAddress}")
      _ <- Async[F].delay { connectFuture.awaitUninterruptibly() }
      result <- writeBuffer(targetAddress, connectFuture, data)
      _ <- result match {
        case Left(error) =>
          logger.error(s"Error while writing the buffer: ${error}")
        case Right(value) => Async[F].unit
      }
      _ <- Async[F].pure { connector.dispose() }
    } yield (result)
  }

}
