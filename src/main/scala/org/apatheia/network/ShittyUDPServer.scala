package org.apatheia.network

import org.apache.mina.transport.socket.DatagramSessionConfig
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor
import java.net.InetSocketAddress
import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import org.apache.mina.core.buffer.IoBuffer

object ShittyUDPServer {
  def main(args: Array[String]): Unit = {
    val acceptor = new NioDatagramAcceptor()
    val config = acceptor.getSessionConfig.asInstanceOf[DatagramSessionConfig]
    config.setReuseAddress(true)
    config.setBroadcast(true)
    config.setReceiveBufferSize(65536)

    acceptor.setHandler(new SimpleUDPServerHandler())
    acceptor.bind(new InetSocketAddress(1234))

    println("UDP server listening on port 1234")
  }

  class SimpleUDPServerHandler extends IoHandlerAdapter {
    override def messageReceived(session: IoSession, message: Any): Unit = {
      val data = message.asInstanceOf[IoBuffer].array()
      val address = session.getRemoteAddress.asInstanceOf[InetSocketAddress]
      println(
        s"Received UDP message from ${address.getAddress.getHostAddress}:${address.getPort}: ${new String(data)}"
      )
    }
  }
}
