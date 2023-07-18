package eu.karnicki.fun.http

import eu.karnicki.fun.Errors.{ServiceCallError, toErrorMessage}
import eu.karnicki.fun.{CounterpartyHash, Errors}
import io.circe.Decoder
import io.netty.channel.{AbstractChannel, ChannelException}
import zio.*
import zio.Schedule.WithState
import zio.http.{Body, Client, Response, ZClient}

import java.net.ConnectException
import scala.language.postfixOps

case class CounterpartyServiceConfig(url: String,
                                     retryStrategy: Schedule[ZClient[Any, Body, Throwable, Response], ServiceCallError, Any])

object CounterpartyServiceConfig:
  lazy val live: ULayer[CounterpartyServiceConfig] =
    ZLayer.succeed(CounterpartyServiceConfig(
      url = "http://localhost:8080/deanonymize/",
      retryStrategy = Schedule.recurs(5) && Schedule.spaced(100 millis) && Schedule.recurWhile(_ == Errors.Transient)))

trait CounterpartyService:
  def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash): zio.ZIO[zio.http.Client, Throwable, String]

object CounterpartyService:
  lazy val live: ZLayer[CounterpartyServiceConfig, Throwable, CounterpartyService] =
    ZLayer.scoped {
      for {
        counterpartyServiceConfig <- ZIO.service[CounterpartyServiceConfig]
      } yield CounterpartyServiceLive(counterpartyServiceConfig)
    }

  final case class CounterpartyServiceLive(counterpartyServiceConfig: CounterpartyServiceConfig) extends CounterpartyService:
    override def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash): zio.ZIO[Client, Throwable, String] =
      Client
        .request(s"${counterpartyServiceConfig.url}$anonymizedCounterpartyId")
        .catchAllTrace {
          case (throwable: ConnectException, trace: StackTrace) =>
            ZIO.logError(s"Transient connection error while deanonymising: ${(throwable, trace).toErrorMessage}") *> ZIO.fail(Errors.Transient)
          case (throwable, trace) =>
            ZIO.logError(s"Error while deanonymising: ${(throwable, trace).toErrorMessage}") *> ZIO.fail(Errors.ResponseError("unknown"))
        }
        .retry(counterpartyServiceConfig.retryStrategy)
        .flatMap(response => response.body.asString)
