package eu.karnicki.fun

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers.shouldBe

class MonoidTest extends AnyFlatSpecLike:
  behavior of "Monoid"
  it should "have an empty value for ints" in:
    Monoid[Int].empty shouldBe 0
