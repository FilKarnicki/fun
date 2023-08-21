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

import java.util.UUID
import scala.io.Source
import scala.language.{existentials, postfixOps}

object ClientApp extends ZIOAppDefault:
  def effects(eventSource: IO[Throwable, Seq[Event]]): ZIO[Client with CounterpartyServiceConfig with CounterpartyService,
    Throwable,
    Seq[Contract]] =
    for {
      counterpartyService <- ZIO.service[CounterpartyService]
      counterpartyServiceConfig <- ZIO.service[CounterpartyServiceConfig]
      counterpartyServiceSemaphore <- Semaphore.make(counterpartyServiceConfig.maxInFlight)
      events <- eventSource
      pricedEventsAndCounterparties <- priceEventsAndGetCounterparties(counterpartyService, counterpartyServiceSemaphore, events)
      enrichedEvents = mapPricedEventsAndCounterpartiesToEnrichedEvents(pricedEventsAndCounterparties)
      contracts = createContracts(enrichedEvents)
    } yield contracts

  private def createContracts(enrichedEvents: Seq[EnrichedEvent]) = {
    enrichedEvents.map { enrichedEvent =>
      (for {
        premium <- cats.data.State[Contract, Instrument](
          contract =>
            val premium = Instrument(UUID.randomUUID.toString, enrichedEvent.price)
            val buyerObligation = Obligation(enrichedEvent.seller, enrichedEvent.buyer, Seq(premium), Status.live)

            (contract.copy(obligations = buyerObligation +: contract.obligations), premium))
        otherLeg <- cats.data.State[Contract, Instrument](
          contract =>
            val otherLeg = Instrument(UUID.randomUUID.toString, enrichedEvent.event.notional)
            val sellerObligation = Obligation(enrichedEvent.seller, enrichedEvent.buyer, Seq(otherLeg), Status.live)

            (contract.copy(obligations = sellerObligation +: contract.obligations), otherLeg))
      } yield otherLeg).run(Contract(UUID.randomUUID.toString, Nil)).value._1
    }
  }

  private def mapPricedEventsAndCounterpartiesToEnrichedEvents(pricedEventsAndCounterparties: Seq[(Event, BigDecimal, Seq[(ClientSide, _root_.java.lang.String)])]) = {
    pricedEventsAndCounterparties.map {
      case (event, price, seq) =>
        seq match
          case (ClientSide.Buyer, client) :: tail =>
            EnrichedEvent(event, client, tail.head._2, price)
          case (ClientSide.Seller, client) :: tail =>
            EnrichedEvent(event, tail.head._2, client, price)
    }
  }

  private def priceEventsAndGetCounterparties(counterpartyService: CounterpartyService, counterpartyServiceSemaphore: Semaphore, events: Seq[Event]) = {
    ZIO.collectAll(
      events.map(event =>
        priceEvent(event)
          .zipPar(ZIO.collectAllPar(
            Seq(
              retrieveCounterpartyEffect(counterpartyService, event.anonymizedBuyer, ClientSide.Buyer, counterpartyServiceSemaphore),
              retrieveCounterpartyEffect(counterpartyService, event.anonymizedSeller, ClientSide.Seller, counterpartyServiceSemaphore)
            )))))
  }

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

  private def priceEvent(event: Event): ZIO[Any, Throwable, (Event, BigDecimal)] = {
    ZIO.succeed(event).zip(pricing(event.notional))
  }

  private def retrieveCounterpartyEffect(counterpartyService: CounterpartyService, counterpartyHash: CounterpartyHash, clientSide: ClientSide, semaphore: Semaphore) = {
    counterpartyService.deanonymize(counterpartyHash, semaphore)
      .orElse(counterpartyService.deanonymize(counterpartyHash.toLowerCase, semaphore))
      .map(returnString => (clientSide, returnString))
      .debugThread
  }

  override def run: URIO[Any, ExitCode] =
    val auto = effects(eventSource("resource.json"))
      .debug
      .provide(
        Client.default,
        CounterpartyService.live,
        CounterpartyServiceConfig.live,
        ZLayer.Debug.mermaid)
      .exitCode

    auto