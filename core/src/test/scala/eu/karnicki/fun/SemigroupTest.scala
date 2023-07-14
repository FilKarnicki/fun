package eu.karnicki.fun

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers.shouldBe

class SemigroupTest extends AnyFlatSpecLike:
  behavior of "Semigroup"
  it should "combine two ints" in:
    import eu.karnicki.fun.Semigroup.*
    1 |+| 2 shouldBe 3
