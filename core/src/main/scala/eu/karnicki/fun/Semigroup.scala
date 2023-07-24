package eu.karnicki.fun

import scala.annotation.targetName

trait Semigroup[A]:
  @targetName("combine")
  def ++(a: A, b: A): A

object Semigroup:
  given Semigroup[Int] with
    @targetName("combine")
    def ++(a: Int, b: Int): Int =
      a + b

  given Semigroup[List[_]] with
    @targetName("combine")
    def ++(a: List[_], b: List[_]): List[_] =
      a ++ b

  given Semigroup[Option[_]] with
    @targetName("combine")
    def ++(a: Option[_], b: Option[_]): Option[_] =
      Option((a,b))

  /**
   * This method makes it so that we don't have to use summon
   * Use example:  Semigroup[Int]
   */
  def apply[A](using instance: Semigroup[A]): Semigroup[A] =
    instance

  extension [T](a: T)
    @targetName("combine")
    inline def |+|(b: T)(using semigroup: Semigroup[T]): T =
      semigroup.++(a, b)