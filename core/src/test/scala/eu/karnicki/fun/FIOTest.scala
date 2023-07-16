package eu.karnicki.fun

import eu.karnicki.fun.FIO
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe

class FIOTest extends AnyFlatSpecLike:
  behavior of "FIO"
  it should "flatMap" in :
    FIO.effect(42)
      .flatMap(a =>
        FIO.effect(s"the meaning of life is $a"))
      .unsafeInterpret.apply shouldBe "the meaning of life is 42"

  it should "map" in :
    FIO.effect(42)
      .map(a =>
        s"the meaning of life is $a")
      .unsafeInterpret.apply shouldBe "the meaning of life is 42"