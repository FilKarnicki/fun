package eu.karnicki.fun

import eu.karnicki.fun.async.Pricing
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
  private val effects = for {
    events <- eventSource("resource.json")
    pricedEvents <- priceEvents(events)
    counterpartyService <- ZIO.service[CounterpartyService]
    pricedEventsAndCounterparties <- ZIO.collectAll(
      pricedEvents.map((event, price) =>
        ZIO.succeed((event, price))
          .zip(
            ZIO.collectAllPar(
              Seq(
                retrieveCounterpartyEffect(counterpartyService, event.anonymizedBuyer, ClientSide.Buyer),
                retrieveCounterpartyEffect(counterpartyService, event.anonymizedSeller, ClientSide.Seller)
              )))))

    enrichedEvents = pricedEventsAndCounterparties.map {
      case (event, price, seq) =>
        seq match
          case (ClientSide.Buyer, client) :: tail =>
            EnrichedEvent(event, client, tail.head._2, price)
          case (ClientSide.Seller, client) :: tail =>
            EnrichedEvent(event, tail.head._2, client, price)
    }
  } yield enrichedEvents

  private def eventSource(resourceJson: String) = ZIO.acquireReleaseWith(
    ZIO.attempt(Source.fromResource(resourceJson))
  )(
    src => ZIO.succeed(src.close)
  )(
    src => for {
      eventString <- ZIO.attempt(src.getLines.mkString)
      events <- ZIO.fromEither(decode[Seq[Event]](eventString))
    } yield events)

  private def pricing(notional: BigDecimal): Task[BigDecimal] =
    ZIO.async(cb =>
      Pricing.priceAsync(notional
      )(
        price => cb(ZIO.succeed(price).debugThread)
      )(
        throwable => cb(ZIO.fail(throwable).debugThread)))

  private def priceEvents(events: Seq[Event]) = {
    ZIO.collectAll(events.map(event => ZIO.succeed(event).zip(pricing(event.notional))))
  }

  private def retrieveCounterpartyEffect(counterpartyService: CounterpartyService, counterpartyHash: CounterpartyHash, clientSide: ClientSide) = {
    counterpartyService.deanonymize(counterpartyHash)
      .orElse(counterpartyService.deanonymize(counterpartyHash.toLowerCase))
      .map(returnString => (clientSide, returnString))
      .debugThread
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