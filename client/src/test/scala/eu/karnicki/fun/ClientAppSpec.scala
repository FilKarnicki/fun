package eu.karnicki.fun

import eu.karnicki.fun.http.{CounterpartyService, CounterpartyServiceConfig}
import zio.http.{Client, TestClient}
import zio.test.*
import zio.{ZIO, *}

object ClientAppSpec extends ZIOSpecDefault:
  private val mockConfig = ZLayer.fromFunction(() => new CounterpartyServiceConfig("", Schedule.recurs(0), 1))
  private val mockService = ZLayer.fromFunction(() => new CounterpartyService {
    override def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash, semaphore: Semaphore): ZIO[Client, Errors.ServiceCallError, String] =
      ZIO.succeed(anonymizedCounterpartyId.toUpperCase)
  })

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("client app")(
      test("it should return enriched trades") {
        val event1 = Event("t1", 1_000_000, "a", "b")
        val effectsToBeTested = ClientApp.effects(ZIO.succeed(Seq(event1)))

        assertZIO(effectsToBeTested)(Assertion.contains(EnrichedEvent(event1, "A", "B", 10_000)))
          .provide(TestClient.layer, mockService, mockConfig)
      },
      test("it should return enriched trades - property based testing") {
        check(
          Gen.stringBounded(0, 99)(Gen.alphaChar),
          Gen.bigDecimal(-1_000_000_000, 1_000_000_000),
          Gen.string(Gen.alphaNumericChar),
          Gen.string(Gen.alphaNumericChar)) {
          (trader, notional, anonBuyer, anonSeller) =>
            val event1 = Event(trader, notional, anonBuyer, anonSeller)
            val effectsToBeTested = ClientApp.effects(ZIO.succeed(Seq(event1)))
            assertZIO(effectsToBeTested)(Assertion.contains(EnrichedEvent(event1, anonBuyer.toUpperCase, anonSeller.toUpperCase, notional / 100)))
              .provide(TestClient.layer, mockService, mockConfig)
        }
      }
    )


