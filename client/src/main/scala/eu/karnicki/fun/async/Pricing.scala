package eu.karnicki.fun.async

import zio.*

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Pricing:
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def priceAsync(notional: BigDecimal)(onSuccess: BigDecimal => Unit)(onFailure: Throwable => Unit): Unit =
    Future {
      notional / 100
    }.onComplete {
      case Failure(exception) =>
        onFailure(exception)
      case Success(value) =>
        onSuccess(value)
    }
