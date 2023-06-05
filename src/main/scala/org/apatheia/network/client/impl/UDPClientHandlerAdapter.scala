package org.apatheia.network.client.impl

import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory
import cats.effect.std.Dispatcher
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.kernel.Sync
import org.slf4j

final case class UDPClientHandlerAdapter() extends IoHandlerAdapter {

  val logger = slf4j.LoggerFactory.getLogger(this.getClass())

  override def exceptionCaught(session: IoSession, cause: Throwable): Unit =
    logger.debug(s"UDP Client Error: ${cause}")

  override def messageSent(session: IoSession, message: Object): Unit =
    logger.debug(
      s"UDP Datagram message was sent to ${session.getRemoteAddress()}"
    )

  override def sessionCreated(session: IoSession): Unit =
    logger.debug(
      s"UDP Client Session created: #${session.getRemoteAddress().toString()}"
    )

  override def sessionClosed(session: IoSession): Unit =
    logger.debug(
      s"UDP Client Session closed: #${session.getRemoteAddress().toString()}"
    )

}
