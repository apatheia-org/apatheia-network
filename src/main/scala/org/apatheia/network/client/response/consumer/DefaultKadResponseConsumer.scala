package org.apatheia.network.client.response.consumer

import cats.effect.kernel.Async
import org.apatheia.network.client.response.store.ResponseStoreRef
import org.apatheia.network.model.{KadResponsePackage, OpId}
import scala.concurrent.duration._
import cats.implicits._
import java.time.LocalDateTime
import cats.instances.duration
import java.time.temporal.TemporalUnit
import java.time.temporal.ChronoUnit

final case class DefaultKadResponseConsumer[F[_]: Async](
    responseKeyStore: ResponseStoreRef[F]
) extends KadResponseConsumer[F] {

  private def retryConsumeResponse(opId: OpId, timeLimit: LocalDateTime)(
      f1: Int = 0,
      f2: Int = 1
  ): F[Option[KadResponsePackage]] = {
    val sleepTime: Int = f2 * 100
    val result = for {
      _ <- Async[F].sleep(sleepTime.millis)
      response <- responseKeyStore.get(opId)
    } yield (response)

    val now: LocalDateTime = LocalDateTime.now()
    if (timeLimit.isAfter(now)) {
      result.orElse(
        retryConsumeResponse(opId, timeLimit)(f2, f1 + f2)
      )
    } else {
      Async[F].pure(None)
    }
  }

  override def consumeResponse(
      opId: OpId,
      timeout: Duration
  ): F[Option[KadResponsePackage]] =
    retryConsumeResponse(
      opId,
      LocalDateTime.now().plus(timeout.toMillis, ChronoUnit.MILLIS)
    )()

}
