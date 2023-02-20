package org.apatheia.network

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.future.ConnectFuture
import org.apache.mina.core.service.IoConnector
import org.apache.mina.transport.socket.DatagramSessionConfig
import org.apache.mina.transport.socket.nio.NioDatagramConnector

import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession

class SimpleUDPClientHandler extends IoHandlerAdapter {
  override def messageReceived(session: IoSession, message: Any): Unit = {
    val buffer = message.asInstanceOf[IoBuffer]
    val bytes = Array.ofDim[Byte](buffer.limit())
    buffer.get(bytes)
    val response = new String(bytes)
    println(s"Received response: $response")
  }

  override def exceptionCaught(session: IoSession, cause: Throwable): Unit = {
    println("Exception caught")
    cause.printStackTrace()
    session.closeNow()
  }
}

object ShittyUDPClient {
  def main(args: Array[String]): Unit = {
    val connector: IoConnector = new NioDatagramConnector()
    connector.setHandler(new SimpleUDPClientHandler())
    val sessionConfig =
      connector.getSessionConfig.asInstanceOf[DatagramSessionConfig]
    sessionConfig.setReuseAddress(true)
    sessionConfig.setBroadcast(true)
    val future: ConnectFuture =
      connector.connect(new InetSocketAddress("localhost", 1234))
    future.awaitUninterruptibly()
    if (future.isConnected) {
      val buffer: IoBuffer = IoBuffer.allocate(50)
      buffer.putString("Hello, World!", StandardCharsets.UTF_8.newEncoder())
      future.getSession.write(buffer)
      future.getSession.getCloseFuture.awaitUninterruptibly()
    } else {
      System.err.println("Could not connect to server")
    }
    connector.dispose()
  }
}
