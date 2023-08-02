package eu.karnicki.fun

import cats.instances.BigDecimalInstances
import cats.syntax.semigroup.*
import eu.karnicki.fun.Monoid.!+!

import scala.annotation.targetName

object RiskCalc:
  implicit val thetaMonoidCats: cats.Monoid[Theta] = cats.Monoid.instance(Theta(1), (a, b) => Theta(a.value * b.value))
  implicit val riskSemigroupCats: cats.Semigroup[Risk] = cats.Semigroup.instance(
    (risk1, risk2) =>
      Risk(
        delta = risk1.delta |+| risk2.delta,
        gamma = risk1.gamma |+| risk2.gamma,
        theta = foldThingsWithCats(Seq(risk1.theta, risk2.theta))))

  given Monoid[Theta] with
    @targetName("combine")
    override def ++(a: Theta, b: Theta): Theta = Theta(a.value * b.value)

    override val empty: Theta = Theta(1)
    override val identity: Theta => Theta = a => Theta(1)

  given Semigroup[Risk] with
    @targetName("combine")
    override def ++(a: Risk, b: Risk): Risk =
      Risk(
        delta = a.delta + b.delta,
        gamma = a.gamma + b.gamma,
        theta = foldThingsWithOwn(Seq(a.theta, b.theta)))

  def reduceThingsWithCats[T](things: Seq[T])(using semigroup: cats.Semigroup[T]): T =
    things.reduce(semigroup.combine)

  def reduceThingsWithOwn[T](things: Seq[T])(using semigroup: Semigroup[T]): T =
    things.reduce(semigroup.++)

  def calcRisk(instruments: Seq[Instrument]): Risk =
    //reduceThingsWithCats(instruments.map(instrument =>
    reduceThingsWithOwn(instruments.map(instrument =>
      Risk(
        delta = instrument.notional / 100,
        gamma = instrument.notional / 1000,
        theta = Theta(instrument.notional / 500_000))))

  def foldThingsWithCats[T](things: Seq[T])(using monoid: cats.Monoid[T]): T =
    things.foldLeft(monoid.empty)(monoid.combine)

  def foldThingsWithOwn[T](things: Seq[T])(using monoid: Monoid[T]): T =
    things.foldLeft(monoid.empty)(monoid.++)