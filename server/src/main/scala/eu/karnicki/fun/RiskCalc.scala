package eu.karnicki.fun

import cats.instances.BigDecimalInstances
import cats.syntax.applicative.*
import cats.syntax.semigroup.*
import eu.karnicki.fun.Monoid.!+!
import zio.schema.Patch.Temporal

import java.time.temporal.{ChronoUnit, TemporalAmount}
import java.time.{Duration, Instant}
import scala.annotation.targetName

object RiskCalc: // completely made up logic, just trying out monads, monoids, semigroups
  implicit val timeSensitiveMonoidCats: cats.Monoid[TimeSensitive[Theta]] = cats.Monoid.instance(
    TimeSensitive(List(Theta(1) -> Instant.now)),
    (a, b) =>
      Seq(
        a.values.maxBy((theta, instant) => instant),
        b.values.maxBy((theta, instant) => instant))
        .minBy((theta, instant) => theta.value)
        ._1.pure)

  implicit val riskSemigroupCats: cats.Semigroup[Risk] = cats.Semigroup.instance(
    (risk1, risk2) =>
      Risk(
        delta = risk1.delta |+| risk2.delta,
        gamma = risk1.gamma |+| risk2.gamma,
        theta = foldThingsWithCats(Seq(risk1.theta, risk2.theta))))

  given timeSensitiveMonadCats: cats.Monad[TimeSensitive] with
    override def pure[A](x: A): TimeSensitive[A] = TimeSensitive(List((x, Instant.now)))

    override def flatMap[A, B](fa: TimeSensitive[A])(f: A => TimeSensitive[B]): TimeSensitive[B] =
      TimeSensitive(
        fa.values.flatMap((a, aTimestamp) =>
          f(a).values.map((b, bTimestamp) =>
            (b, Seq(aTimestamp, bTimestamp).min))))

    override def tailRecM[A, B](a: A)(f: A => TimeSensitive[Either[A, B]]): TimeSensitive[B] =
      ???

  given Monoid[TimeSensitive[Theta]] with
    @targetName("combine")
    override def ++(a: TimeSensitive[Theta], b: TimeSensitive[Theta]): TimeSensitive[Theta] =
      TimeSensitive(a.values ++ b.values)

    override val empty: TimeSensitive[Theta] = TimeSensitive(List(Theta(1) -> Instant.now))
    override val identity: TimeSensitive[Theta] => TimeSensitive[Theta] = a => TimeSensitive(List(Theta(1) -> Instant.now))

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

  import cats.Monad.*
  import cats.syntax.applicative.*
  import cats.syntax.flatMap.*
  import cats.syntax.monad.*

  def calcRisk(instruments: Seq[Instrument]): Risk =
    reduceThingsWithCats(instruments.map(instrument =>
      //reduceThingsWithOwn(instruments.map(instrument =>
      val mockTheta =
        TimeSensitive(
          (Theta(instrument.notional / 500_000) -> Instant.now.minus(Duration.ofDays(3))
            ) :: (Theta(instrument.notional / 400_000) -> Instant.now.minus(Duration.ofDays(2))
            ) :: (Theta(instrument.notional / 300_000) -> Instant.now.minus(Duration.ofDays(1))
            ) :: Nil
        ).flatMap(theta =>
          Theta(theta.value * 0.1).pure)
      Risk(
        delta = instrument.notional / 100,
        gamma = instrument.notional / 1000,
        theta = mockTheta)))

  def foldThingsWithCats[T](things: Seq[T])(using monoid: cats.Monoid[T]): T =
    things.foldLeft(monoid.empty)(monoid.combine)

  def foldThingsWithOwn[T](things: Seq[T])(using monoid: Monoid[T]): T =
    things.foldLeft(monoid.empty)(monoid.++)