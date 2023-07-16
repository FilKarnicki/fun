package eu.karnicki.fun

object FIO:
  def effect[A](a: A): FIO[A] =
    FIO(() => a)

class FIO[A](val unsafeInterpret: () => A):
  def map[B](fun: A => B): FIO[B] =
    flatMap(fun.andThen(FIO.effect))
  def flatMap[B](fun: A => FIO[B]): FIO[B] =
    FIO.effect(fun(unsafeInterpret())).unsafeInterpret()