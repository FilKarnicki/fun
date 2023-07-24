package eu.karnicki.fun

import eu.karnicki.fun.Semigroup.*
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers.shouldBe

class SemigroupTest extends AnyFlatSpecLike:
  behavior of "Semigroup"
  it should "combine two int functions" in :
    1 |+| 2 shouldBe 3
  it should "be associative" in :
    (1 |+| 2) |+| 3 shouldBe 1 |+| (2 |+| 3)