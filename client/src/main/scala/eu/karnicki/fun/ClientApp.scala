package eu.karnicki.fun

import eu.karnicki.fun.http.CounterpartyService.CounterpartyServiceLive
import eu.karnicki.fun.http.{CounterpartyService, CounterpartyServiceConfig}
import io.circe.Decoder
import zio.*
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.{Client, DnsResolver, Response, ZClient}

import scala.language.{existentials, postfixOps}

object ClientApp extends ZIOAppDefault:
  private val events = Seq(
    Event(
      traderId = "t1",
      notional = 1_000_000,
      anonymizedBuyer = "a",
      anonymizedSeller = "b"),
    Event("t1", 800_000, "B", "A"))

  private val eventHashTuples =
    events
      .map(event =>
        (event, event.anonymizedBuyer, event.anonymizedSeller))

  private val effects = ZIO.scoped {
    for {
      counterpartyService <- ZIO.service[CounterpartyService]
      eventsAndResolved <- ZIO.collectAll(
        eventHashTuples.map((event, buyer, seller) =>
          ZIO.succeed(event)
            .zip(
              ZIO.collectAllPar(
                Seq(
                  counterpartyService.deanonymize(buyer)
                    .orElse(counterpartyService.deanonymize(buyer.toLowerCase))
                    .map(returnString => (ClientSide.Buyer, returnString))
                    .debugThread,
                  counterpartyService.deanonymize(seller)
                    .orElse(counterpartyService.deanonymize(seller.toLowerCase))
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