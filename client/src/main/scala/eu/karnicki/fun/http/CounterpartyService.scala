package eu.karnicki.fun.http

import eu.karnicki.fun.CounterpartyHash
import io.circe.Decoder
import zio.*

case class CounterpartyServiceConfig(url: String)

val counterpartyServiceConfigZLayer = ZLayer.succeed(CounterpartyServiceConfig("http://localhost:8080/deanonymize/"))

trait CounterpartyService:
  def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash): Task[T]

