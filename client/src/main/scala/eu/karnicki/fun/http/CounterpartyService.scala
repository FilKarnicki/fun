package eu.karnicki.fun.http

import eu.karnicki.fun.Errors.{ServiceCallError, toErrorMessage}
import eu.karnicki.fun.{CounterpartyHash, Errors}
import io.circe.Decoder
import io.netty.channel.{AbstractChannel, ChannelException}
import zio.*
import zio.Schedule.WithState
import zio.http.*

import java.net.ConnectException
import scala.language.postfixOps

case class CounterpartyServiceConfig(url: String,
                                     retryStrategy: Schedule[ZClient[Any, Body, Throwable, Response], ServiceCallError, Any],
                                     maxInFlight: Int)

object CounterpartyServiceConfig:
  lazy val live: ULayer[CounterpartyServiceConfig] =
    ZLayer.succeed(CounterpartyServiceConfig(
      url = "http://localhost:8080/deanonymize/",
      retryStrategy = Schedule.recurs(5) && Schedule.exponential(100 millis, 1.2) && Schedule.recurWhile(_ == Errors.Transient),
      maxInFlight = 1))

trait CounterpartyService:
  def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash, semaphore: Semaphore): zio.ZIO[zio.http.Client, ServiceCallError, String]

object CounterpartyService:
  def create(config: CounterpartyServiceConfig): CounterpartyService = CounterpartyServiceLive(config)

  val live: ZLayer[CounterpartyServiceConfig, Nothing, CounterpartyService] = ZLayer.fromFunction(create)

  final case class CounterpartyServiceLive(counterpartyServiceConfig: CounterpartyServiceConfig) extends CounterpartyService:
    override def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash, semaphore: Semaphore): ZIO[Client, ServiceCallError, String] =
      semaphore.withPermit {
        Client.request(s"${counterpartyServiceConfig.url}$anonymizedCounterpartyId")
          .foldZIO({
            case throwable: ConnectException =>
              ZIO.logError(
                s"Transient connection error while deanonymizing: ${throwable.getMessage}") *> ZIO.fail(Errors.Transient)
            case throwable =>
              ZIO.logError(
                s"Error while deanonymizing: ${throwable.getMessage}") *> ZIO.fail(Errors.ResponseError(throwable.getMessage))
          }, successfulResponse => {
            successfulResponse.status match
              case Status.NotFound =>
                ZIO.fail(Errors.NotFound)
              case Status.Ok =>
                successfulResponse.body.asString.orDie
              case status =>
                ZIO.fail(Errors.ResponseError(status.text))
          })
          .retry(counterpartyServiceConfig.retryStrategy >>> Schedule.elapsed.mapZIO(elapsed => ZIO.logInfo(s"Backed off retrying for: $elapsed")))
      }
