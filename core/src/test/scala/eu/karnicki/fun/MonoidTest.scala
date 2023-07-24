package eu.karnicki.fun

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers.shouldBe

class MonoidTest extends AnyFlatSpecLike:
  behavior of "Monoid"
  it should "have an an identity function for ints" in:
    Monoid[Int].identity(1) shouldBe 1
  it should "have an empty element for ints" in:
    Monoid[Int].empty shouldBe 0
