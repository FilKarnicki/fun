package eu.karnicki.fun

import scala.annotation.targetName

trait Semigroup[T]:
  @targetName("combine")
  def ++(a: T, b: T): T

object Semigroup:
  given Semigroup[Int] with
    @targetName("combine")
    def ++(a: Int, b: Int): Int =
      a + b

  /**
   * This method makes it so that we don't have to use summon
   * Use example:  Semigroup[Int]
   */
  def apply[T](using instance: Semigroup[T]): Semigroup[T] =
    instance

  extension [T](a: T)
    @targetName("combine")
    inline def |+|(b: T)(using semigroup: Semigroup[T]): T =
      semigroup ++ (a, b)