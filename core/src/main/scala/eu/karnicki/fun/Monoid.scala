package eu.karnicki.fun

import scala.annotation.targetName

trait Monoid[T] extends Semigroup[T]:
  val empty: T

object Monoid:
  given Monoid[Int] with
    override val empty: Int =
      0

    @targetName("combine")
    override def ++(a: Int, b: Int): Int =
      Semigroup[Int].++(a, b)

  /**
   * This method makes it so that we don't have to use summon
   * Use example:  Monoid[Int]
   */
  def apply[T](using instance: Monoid[T]): Monoid[T] =
    instance