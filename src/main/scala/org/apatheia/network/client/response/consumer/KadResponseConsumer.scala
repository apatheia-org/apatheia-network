package org.apatheia.network.client.response.consumer

import org.apatheia.network.model.OpId
import scala.concurrent.duration.Duration
import org.apatheia.network.model.KadResponsePackage

trait KadResponseConsumer[F[_]] {
  def consumeResponse(
      opId: OpId,
      timeout: Duration
  ): F[Option[KadResponsePackage]]
}
