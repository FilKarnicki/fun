package eu.karnicki.fun.async

import zio.*

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Pricing:
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private def getRate: BigDecimal = BigDecimal(scala.util.Random.nextDouble * 10)

  private def nowPremium(rate: BigDecimal) = cats.Eval.now(rate * scala.util.Random.nextDouble)

  // some made up logic
  def priceAsync(notional: BigDecimal)(onSuccess: BigDecimal => Unit)(onFailure: Throwable => Unit): Unit =
    Future {
      val oneRate = cats.Eval.later {
        println(s"getting rate once for $notional")
        getRate
      }

      val price = for {
        rate <- oneRate
      } yield rate * notional / 100

      val premium = for {
        rate <- oneRate
        premium <- nowPremium(rate)
      } yield premium

      price.value + premium.value
    }.onComplete {
      case Failure(exception) =>
        onFailure(exception)
      case Success(value) =>
        onSuccess(value)
    }
