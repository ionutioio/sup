package sup

import cats.Id
import cats.effect.implicits.catsEffectSyntaxConcurrent
import cats.effect.{Concurrent, Sync, Timer}
import com.datastax.oss.driver.api.core.CqlSession

import scala.concurrent.duration.FiniteDuration

object cassandra {

  def connectionCheck[F[_] : Sync : Concurrent](session: CqlSession, timeout: FiniteDuration)(implicit timer: Timer[F]): HealthCheck[F, Id] =
    HealthCheck.liftFBoolean {
      Sync[F].delay(session
        .execute("SELECT now() FROM system.local").isFullyFetched
      ).timeoutTo(timeout, Sync[F].pure(false))
    }
}
