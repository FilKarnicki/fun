package eu.karnicki.fun.taglessfinal

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.funsuite.AnyFunSuite

class FIOTest extends AnyFlatSpecLike:
  behavior of "FIO"
  it should "succeed in a simplest test" in:
    assert("Scala".toLowerCase == "scala")