package eu.karnicki.fun

import eu.karnicki.fun.http.CounterpartyService.CounterpartyServiceLive
import eu.karnicki.fun.http.{CounterpartyService, CounterpartyServiceConfig}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import zio.*
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.{Client, DnsResolver, Response, ZClient}

import scala.io.Source
import scala.language.{existentials, postfixOps}

object ClientApp extends ZIOAppDefault:
  private val effects = ZIO.scoped {
    val eventSourceZio = ZIO.acquireRelease(
      ZIO.attempt(Source.fromResource("resource.json")))(finalizingSource => ZIO.succeed(finalizingSource.close))

    for {
      eventSource <- eventSourceZio
      eventString <- ZIO.attempt(eventSource.getLines.mkString)
      events <- ZIO.fromEither(decode[Seq[Event]](eventString))
      counterpartyService <- ZIO.service[CounterpartyService]
      eventsAndResolved <- ZIO.collectAll(
        events.map(event =>
          ZIO.succeed(event)
            .zip(
              ZIO.collectAllPar(
                Seq(
                  counterpartyService.deanonymize(event.anonymizedBuyer)
                    .orElse(counterpartyService.deanonymize(event.anonymizedBuyer.toLowerCase))
                    .map(returnString => (ClientSide.Buyer, returnString))
                    .debugThread,
                  counterpartyService.deanonymize(event.anonymizedSeller)
                    .orElse(counterpartyService.deanonymize(event.anonymizedSeller))
                    .map(returnString => (ClientSide.Seller, returnString))
                    .debugThread)))))

      enrichedEvents = eventsAndResolved.map {
        case (event, seq) =>
          seq match
            case (ClientSide.Buyer, client) :: tail =>
              EnrichedEvent(event, client, tail.head._2)
            case (ClientSide.Seller, client) :: tail =>
              EnrichedEvent(event, tail.head._2, client)
      }
    } yield enrichedEvents
  }

  override def run: URIO[Any, ExitCode] =
    val auto = effects
      .debug
      .provide(
        Client.default,
        CounterpartyService.live,
        CounterpartyServiceConfig.live,
        ZLayer.Debug.mermaid)
      .exitCode

    auto