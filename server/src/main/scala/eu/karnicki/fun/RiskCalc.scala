package eu.karnicki.fun

import scala.annotation.targetName

object RiskCalc:
  implicit val riskSemigroupCats: cats.Semigroup[Risk] = cats.Semigroup.instance {
    (risk1, risk2) =>
      Risk(
        delta = risk1.delta + risk2.delta,
        gamma = risk1.gamma + risk2.gamma)
  }

  given Semigroup[Risk] with
    @targetName("combine")
    override def ++(a: Risk, b: Risk): Risk =
      Risk(
        delta = a.delta + b.delta,
        gamma = a.gamma + b.gamma)

  def reduceThingsWithCats[T](things: Seq[T])(using semigroup: cats.Semigroup[T]): T =
    things.reduce(semigroup.combine)

  def reduceThingsWithOwn[T](things: Seq[T])(using semigroup: Semigroup[T]): T =
    things.reduce(semigroup.++)

  def calcRisk(instruments: Seq[Instrument]): Risk =
  //reduceThingsWithCats(instruments.map(instrument =>
    reduceThingsWithOwn(instruments.map(instrument =>
      Risk(
        delta = instrument.notional / 100,
        gamma = instrument.notional / 1000
      )))
