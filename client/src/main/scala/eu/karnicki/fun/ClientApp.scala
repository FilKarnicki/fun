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
    Event("t1", 800_000, "b", "z"))

  private val eventHashTuples =
    events
      .map(event =>
        (event, event.anonymizedBuyer, event.anonymizedSeller))

  private def joinAllFibers(fibers: Seq[Fiber[Throwable, String]]) =
    ZIO.collectAll(fibers.map(fiber => fiber.join))

  private val effects = ZIO.scoped {
    for {
      counterpartyService <- ZIO.service[CounterpartyService]
      eventsAndResolved <- ZIO.collectAll(
        eventHashTuples.map((event, buyer, seller) =>
          ZIO.succeed(event).zip(
            ZIO.collectAll(
              Seq(
                counterpartyService.deanonymize(buyer).fork,
                counterpartyService.deanonymize(seller).fork))
              .flatMap(joinAllFibers))))

      enrichedEvents = eventsAndResolved.map {
        case (event, resolvedBuyer +: resolvedSeller +: _) =>
          EnrichedEvent(event, resolvedBuyer, resolvedSeller)
      }
    } yield enrichedEvents
  }

  override def run: URIO[Any, ExitCode] =
    effects
      .debug
      .provide(
        Client.default,
        CounterpartyServiceConfig.live,
        CounterpartyService.live)
      .exitCode