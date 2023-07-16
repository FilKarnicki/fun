package eu.karnicki.fun

import zio.*

import scala.language.postfixOps

object IgnoreMe:
  val shower = ZIO.succeed("taking a shower")
  val water = ZIO.succeed("boiling water")
  val waterWithTime = water.debug(printDebugThread) *> ZIO.sleep(5 seconds) *> ZIO.succeed("boiled!")
  val coffee = ZIO.succeed("preparing coffee")
  val alice = ZIO.succeed("call from Alice")

  def printDebugThread = s"[${Thread.currentThread().getName}]"

  val res1 = for
    _ <- shower.debug(printDebugThread)
    _ <- water.debug(printDebugThread)
  yield ()

  val res2 =
    shower
      .flatMap(_ => water)
      .map(_ => ())

  def concurrentTwo() = for {
    _ <- shower.debug(printDebugThread).fork
    _ <- water.debug(printDebugThread)
    _ <- coffee.debug(printDebugThread)
  } yield ()

  def concurrentRoutine() = for {
    showerFiber <- shower.debug(printDebugThread).fork
    waterFiber <- water.debug(printDebugThread).fork
    zipped = showerFiber.zip(waterFiber)
    result <- zipped.join.debug(printDebugThread)
    _ <- ZIO.succeed(s"result: $result") *> coffee.debug(printDebugThread)
  } yield ()

  def concurrentRoutine2() = for {
    _ <- shower.debug(printDebugThread)
    boilingFiber <- waterWithTime.fork
    _ <- alice.debug(printDebugThread).fork *> boilingFiber.interrupt.debug(printDebugThread)
    _ <- ZIO.succeed("screw coffee, going with alice").debug(printDebugThread)

  } yield ()

  val prepareCofeeWithTime = ZIO.succeed("preparing coffee").debug(printDebugThread) *> ZIO.sleep(5 seconds) *> ZIO.succeed("coffee ready1!!!")

  def con3 = for {
    _ <- shower.debug(printDebugThread)
    _ <- water.debug(printDebugThread)
    coffeeFiber <- prepareCofeeWithTime.debug(printDebugThread).fork.uninterruptible
    result <- alice.debug(printDebugThread).fork *> coffeeFiber.interrupt.debug(printDebugThread)
    _ <- result match
      case Exit.Success(value) =>
        ZIO.succeed("sorry alice, making coffee at home").debug(printDebugThread)
      case Exit.Failure(cause) =>
        ZIO.succeed("going to a caffee with alice").debug(printDebugThread)
  } yield ()

  def con32 =
    shower.debug(printDebugThread)
      .flatMap(_ => water.debug(printDebugThread))
      .flatMap(_ => prepareCofeeWithTime.debug(printDebugThread).fork.uninterruptible)
      .flatMap(coffeeFiber => alice.debug(printDebugThread).fork *> coffeeFiber.interrupt.debug(printDebugThread))
      .map {
        case Exit.Success(value) =>
          ZIO.succeed("sorry alice, making coffee at home").debug(printDebugThread)
        case Exit.Failure(cause) =>
          ZIO.succeed("going to a caffee with alice").debug(printDebugThread)
      }
