package eu.karnicki.fun

import scala.annotation.targetName

trait SimpleMonad[A] extends Monoid[A]:
  val a: A
  val empty: A
  def unit[T](a: T): SimpleMonad[T]
  def flatMap[B](ma: A)(f: A => SimpleMonad[B]): SimpleMonad[B]
  def map[B](ma: A)(f: A => B): SimpleMonad[B] =
    flatMap(ma)(f.andThen(unit))

object SimpleMonad:
  def apply[A](using m: SimpleMonad[A]): SimpleMonad[A] = m


class IntSimpleMonad(val a: Int) extends SimpleMonad[Int]:
  override val empty: Int =
    0
  override def flatMap[B](ma: Int)(f: Int => SimpleMonad[B]): SimpleMonad[B] =
    f(ma)
  override val identity: Int => Int =
    a => a

  override def unit[T](a: T): SimpleMonad[T] = ???

  @targetName("combine")
  override def ++(a: Int, b: Int): Int =
    a + b
object IntSimpleMonad:
  def unit(a: Int): SimpleMonad[Int] =
    new IntSimpleMonad(a)

trait Monad[F[_]]:
  def unit[A](a: A): F[A]
  def compose[A, B, C](leftKleisli: A => F[B])(rightKleisli: B => F[C]): A => F[C]
  def flatMap[A, B](ma: F[A])(f: A => F[B]): F[B]
  def map[A, B](ma: F[A])(f: A => B): F[B] =
    flatMap(ma)(f.andThen(unit))

object Monad:
  def apply[M[_]](using m: Monad[M]): Monad[M] = m

trait OptionMonad[A] extends Monad[[T] =>> Option[T]]
given optionMonad: OptionMonad[Int] with
  override def unit[A](a: A): Option[A] = Option(a)

  override def compose[A, B, C](leftKleisli: A => Option[B])(rightKleisli: B => Option[C]): A => Option[C] = ???

  override def flatMap[A, B](ma: Option[A])(f: A => Option[B]): Option[B] =
    ma match
      case Some(value) =>
        f(value)
      case None =>
        None

object Test:
  def main(args: Array[String]): Unit = {
    val something = summon[OptionMonad[Int]].unit(1).flatMap(b => Some(b + 1)).map(x => x * 10)
    println(something.get)
  }

trait Free[M[_], A] extends Monoid[A]:
  import Free.*

  def flatMap[B](f: A => Free[M, B]): Free[M, B]
  def map[B](f: A => B): Free[M, B] =
    flatMap(a => pure(f(a)))

object Free:
  def pure[M[_], A](a: A): Free[M, A] = ???
  def lift[M[_], A](ma: M[A]): Free[M, A] = ???
