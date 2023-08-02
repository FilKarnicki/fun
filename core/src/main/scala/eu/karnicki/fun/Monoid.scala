package eu.karnicki.fun

import scala.annotation.targetName

trait Monoid[T] extends Semigroup[T]:
  val identity: T => T
  val empty: T

object Monoid:
  given Monoid[Int] with
    override val identity: Int=>Int =
      i => i
    override val empty: Int =
      0

    @targetName("combine")
    override def ++(a: Int, b: Int): Int =
      a + b

  /**
   * This method makes it so that we don't have to use summon
   * Use example:  Monoid[Int]
   */
  def apply[T](using instance: Monoid[T]): Monoid[T] =
    instance

  extension [T] (a: T)
    inline def !+!(b: T)(using monoid: Monoid[T]) : T =
      monoid.++(a,b)