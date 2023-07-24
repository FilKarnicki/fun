package eu.karnicki.fun

object KindsAndTypeLambdas:
  class SomethingFunctor[F[_], G[_]]
  val level0FromLevel2Functor = new SomethingFunctor[List, Option]

  class Formatter[F[_], T]
  val level0FromLevel2And1Formatter = new Formatter[Option, String]

  type StringMap = [T] =>> Map[String, T]
  val stringMap: StringMap[Int] = Map()

  type OptionEither = [E, A] =>> Either[E, Option[A]]
  val optionEither: OptionEither[Throwable, Int] = Right[Throwable, Option[Int]](Some(1))

  class EitherMonad[E] extends Monad[[T] =>> Either[E, T]]:
    override def unit[A](a: A): Either[E, A] = ???
    override def compose[A, B, C](leftKleisli: A => Either[E, B])(rightKleisli: B => Either[E, C]): A => Either[E, C] = ???
    override def flatMap[A, B](ma: Either[E, A])(f: A => Either[E, B]): Either[E, B] = ???
    override def map[A, B](ma: Either[E, A])(f: A => B): Either[E, B] = ???

  val runtimeErrorEitherMonad = new EitherMonad[RuntimeException]
  val stringRuntimeEitherMonad: Either[RuntimeException, String] =
    runtimeErrorEitherMonad
      .unit(1)
      .map(one =>
        one.toString)
