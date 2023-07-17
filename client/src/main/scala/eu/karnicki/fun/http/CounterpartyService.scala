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

//val counterpartyServiceConfigZLayer = ZLayer.succeed(CounterpartyServiceConfig("http://localhost:8080/deanonymize/"))
//val poop: WithState[((Long, Long), Unit), Any, Nothing, (Long, Long)] = Schedule.recurs(5) && Schedule.spaced(100 millis) && Schedule.recurWhile(_ == Errors.Transient)
trait CounterpartyService:
  def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash): ZIO[Client, ServiceCallError, Response]

object CounterpartyService:
  lazy val live: ZLayer[Client, Throwable, Response] = ???

  final case class CounterpartyServiceLive(counterpartyServiceConfig: CounterpartyServiceConfig) extends CounterpartyService:
    override def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash) =
      Client.request(s"${counterpartyServiceConfig.url}$anonymizedCounterpartyId")
        .catchAllTrace {
          case (throwable: ConnectException, trace: StackTrace) =>
            ZIO.logError(s"Transient connection error while deanonymising: ${(throwable, trace).toErrorMessage}") *> ZIO.fail(Errors.Transient)
          case (throwable, trace) =>
            ZIO.logError(s"Error while deanonymising: ${(throwable, trace).toErrorMessage}") *> ZIO.fail(Errors.ResponseError("unknown"))
        }
        .retry(counterpartyServiceConfig.retryStrategy)
