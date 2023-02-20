package org.apatheia.network.server.impl

import cats.effect.std.Dispatcher
import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import org.apatheia.network.model.UDPDatagram
import java.net.InetSocketAddress
import org.apache.mina.core.buffer.IoBuffer
import cats.data.EitherT

import scala.util.Try
import cats.implicits._
import cats.effect.kernel.Async
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.apatheia.network.server.UDPDatagramReceiver
import org.apatheia.network.error.UDPServerError

class UDPServerHandlerAdapter[F[_]: Async](
    dispatcher: Dispatcher[F],
    receiver: UDPDatagramReceiver[F]
) extends IoHandlerAdapter {

  val logger = Slf4jLogger.getLogger[F]

  private def extract[A](
      a: => A,
      updError: UDPServerError
  ): EitherT[F, UDPServerError, A] =
    EitherT
      .fromEither[F](
        Try(a).toEither
      )
      .leftFlatMap(_ => EitherT.leftT[F, A](updError))

  private def extractLocalAddress(
      session: IoSession
  ): EitherT[F, UDPServerError, InetSocketAddress] = extract[InetSocketAddress](
    session.getLocalAddress.asInstanceOf[InetSocketAddress],
    UDPServerError.ExtractAddressError
  )

  private def extractSenderAddress(
      session: IoSession
  ): EitherT[F, UDPServerError, InetSocketAddress] = extract[InetSocketAddress](
    session.getRemoteAddress.asInstanceOf[InetSocketAddress],
    UDPServerError.ExtractAddressError
  )

  private def extractData(
      message: Any
  ): EitherT[F, UDPServerError, Array[Byte]] = extract[Array[Byte]](
    message.asInstanceOf[IoBuffer].array(),
    UDPServerError.ExtractBufferError
  )

  private def receiveDatagram(
      localAddress: InetSocketAddress,
      senderAddress: InetSocketAddress,
      data: Array[Byte]
  ): EitherT[F, UDPServerError, UDPDatagram] = for {
    udpDatagram <- EitherT.rightT[F, UDPServerError](
      UDPDatagram(
        from = senderAddress,
        to = localAddress,
        data = data
      )
    )
    _ <- EitherT.right(receiver.onUDPDatagramReceived(udpDatagram))
  } yield (udpDatagram)

  private def logError(udpError: UDPServerError): F[Unit] =
    logger.error(s"UDP Receiving Error: ${udpError.message}")

  private def logSuccess(udpDatagram: UDPDatagram): F[Unit] = logger.debug(
    s"UDP Datagram successfully received: ${udpDatagram.toString()}"
  )

  override def messageReceived(session: IoSession, message: Object): Unit = {
    val receivedDatagram = (for {
      localAddress <- extractLocalAddress(session)
      senderAddress <- extractSenderAddress(session)
      data <- extractData(message)
      datagram <- receiveDatagram(
        localAddress = localAddress,
        senderAddress = senderAddress,
        data = data
      )
    } yield (datagram)).value.flatMap {
      _ match {
        case Right(datagram) => logSuccess(datagram)
        case Left(e)         => logError(e)
      }
    }

    dispatcher.unsafeRunAndForget(receivedDatagram)
  }

}
