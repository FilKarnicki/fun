package eu.karnicki.fun

object ClientApp:
  @main def main(args: String*): Unit =
    import eu.karnicki.fun.Semigroup.*
    println(s"Expected Semigroup combine result: 3. Actual: ${1 |+| 2}")