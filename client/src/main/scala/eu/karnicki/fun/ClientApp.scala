package eu.karnicki.fun

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
    Event("t1", 800_000, "b", "a"))

  private val eventHashTuples =
    events
      .map(event =>
        (event, event.anonymizedBuyer, event.anonymizedSeller))

  private val effects = for {
    eventsWithFibers <- ZIO.collectAll(
      eventHashTuples.map((event, buyer, seller) =>
        ZIO.succeed(event).zip(ZIO.collectAll(
          Seq(
            Client.request(s"http://localhost:8080/deanonymize/$buyer").fork,
            Client.request(s"http://localhost:8080/deanonymize/$seller").fork)))))
    
    eventsAndResolved <- ZIO.collectAll(
      eventsWithFibers.map((event, fiberSeq) =>
        ZIO.succeed(event).zip(ZIO.collectAll(fiberSeq.map(_.join.flatMap(_.body.asString))))))

    enrichedEvents = eventsAndResolved.map {
      case (event, resolvedBuyer +: resolvedSeller +: _) =>
        EnrichedEvent(event, resolvedBuyer, resolvedSeller)
    }
  } yield enrichedEvents

  override def run: URIO[Any, ExitCode] =
    effects.debug.provide(Client.default).exitCode