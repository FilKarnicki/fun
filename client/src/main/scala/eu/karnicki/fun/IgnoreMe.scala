package eu.karnicki.fun

import zio.*

import scala.language.postfixOps

object IgnoreMe:
  def racePair[R, E, A, B](
                            zio1: => ZIO[R, E, A],
                            zio2: => ZIO[R, E, B]): ZIO[R, Nothing, Either[(Exit[E, A], Fiber[E, B]), (Fiber[E, A], Exit[E, B])]] =
    ZIO.uninterruptibleMask(restore => for {
      promise <- Promise.make[Nothing, Either[Exit[E, A], Exit[E, B]]]
      fiberA <- zio1.onExit(outcomeA => promise.succeed(Left(outcomeA))).fork
      fiberB <- zio2.onExit(outcomeB => promise.succeed(Right(outcomeB))).fork
      result <- restore(promise.await).onInterrupt {
        for {
          iA <- fiberA.interrupt.fork
          iB <- fiberB.interrupt.fork
          _ <- iA.join
          _ <- iB.join
        } yield ()
      }
    } yield result match
      case Left(value) => Left(value, fiberB)
      case Right(value) => Right(fiberA, value))


import zio.*

import scala.collection.immutable.Queue

abstract class Mutex {
  def acquire: UIO[Unit]

  def release: UIO[Unit]
}

object Mutex {
  type Signal = Promise[Nothing, Unit]

  case class State(locked: Boolean, waiting: Queue[Signal])

  val unlocked = State(locked = false, Queue())

  def make: UIO[Mutex] = Ref.make(unlocked).map(createInterruptibleMutex)

  def createInterruptibleMutex(state: Ref[State]) =
    new Mutex {
      override def acquire = ZIO.uninterruptibleMask { restore =>
        Promise.make[Nothing, Unit].flatMap { signal =>

          val cleanup: UIO[Unit] = state.modify {
            case State(flag, waiting) =>
              val newWaiting = waiting.filterNot(_ eq signal)
              // blocked only if newWaiting != waiting => release the mutex
              val wasBlocked = newWaiting != waiting
              val decision = if (wasBlocked) ZIO.unit else release

              decision -> State(flag, newWaiting)
          }.flatten

          state.modify {
            case State(false, _) => ZIO.unit -> State(locked = true, Queue())
            case State(true, waiting) => restore(signal.await).onInterrupt(cleanup) -> State(locked = true, waiting.enqueue(signal))
          }.flatten
        }
      }

      override def release = state.modify {
        case State(false, _) => ZIO.unit -> unlocked
        case State(true, waiting) =>
          if (waiting.isEmpty) ZIO.unit -> unlocked
          else {
            val (first, rest) = waiting.dequeue
            first.succeed(()).unit -> State(locked = true, rest)
          }
      }.flatten
    }
}