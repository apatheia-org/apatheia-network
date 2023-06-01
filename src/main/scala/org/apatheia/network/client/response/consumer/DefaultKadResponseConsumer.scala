package org.apatheia.network.client.response.consumer

import cats.effect.kernel.Async
import cats.implicits._
import cats.instances.duration
import org.apatheia.network.client.response.store.ResponseStoreRef
import org.apatheia.network.model.KadResponsePackage
import org.apatheia.network.model.OpId
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import scala.concurrent.duration._

final case class DefaultKadResponseConsumer[F[_]: Async](
    responseKeyStore: ResponseStoreRef[F]
) extends KadResponseConsumer[F] {

  private val logger = Slf4jLogger.getLogger[F]

  private def retryConsumeResponse(opId: OpId, timeLimit: LocalDateTime)(
      f1: Int = 0,
      f2: Int = 1
  ): F[Option[KadResponsePackage]] = {
    val sleepTime: Int = f2 * 1000
    for {
      _ <- Async[F].sleep(sleepTime.millis)
      response <- responseKeyStore.get(opId)
      retry <-
        if (response.isDefined) {
          Async[F].pure(response)
        } else if (timeLimit.isAfter(now())) {
          retryConsumeResponse(opId, timeLimit)(f2, f1 + f2)
        } else {
          logger
            .debug(
              s"Consume response operation for ${opId.value.toString()} has breached the configured timeout"
            )
            .flatMap(_ => Async[F].pure(None))
        }
    } yield (response)
  }

  override def consumeResponse(
      opId: OpId,
      timeout: Duration
  ): F[Option[KadResponsePackage]] =
    retryConsumeResponse(
      opId,
      now().plus(timeout.toMillis, ChronoUnit.MILLIS)
    )()

}
