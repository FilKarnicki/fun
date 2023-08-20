package eu.karnicki.fun.async

import zio.*

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode
import scala.util.{Failure, Success}

object Pricing:
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private def getRate: BigDecimal =
    println(s"Calling a service to get a rate")
    Thread.sleep(1000)
    BigDecimal(scala.util.Random.nextDouble * 10)

  private def nowPremium(rate: BigDecimal) = cats.Eval.now(rate * scala.util.Random.nextDouble)

  // some made up logic
  def priceAsync(notional: BigDecimal)(onSuccess: BigDecimal => Unit)(onFailure: Throwable => Unit): Unit =
    Future {
      val priceLeg = cats.Eval.always(getRate)
        .map(rate =>
          rate * notional / 1_000)
        .memoize
        .map(premium =>
          (premium + scala.util.Random.nextDouble).setScale(4, RoundingMode.UP))

      // called twice but rate only retrieved once
      val priceLegsWithSameRate = for {
        priceLeg1 <- priceLeg
        priceLeg2 <- priceLeg
      } yield priceLeg1 + priceLeg2 * -1

      priceLegsWithSameRate.value
    }.onComplete {
      case Failure(exception) =>
        onFailure(exception)
      case Success(value) =>
        onSuccess(value)
    }
