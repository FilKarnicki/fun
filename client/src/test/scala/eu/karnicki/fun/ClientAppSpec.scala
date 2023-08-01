package eu.karnicki.fun

import eu.karnicki.fun.http.{CounterpartyService, CounterpartyServiceConfig}
import zio.*
import zio.http.{Client, TestClient}
import zio.test.*

object ClientAppSpec extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("client app"):
      test("it should return enriched trades"):
        val event1 = Event("t1", 1_000_000, "a", "b")
        val effectsToBeTested = ClientApp.effects(ZIO.succeed(Seq(event1)))

        assertZIO(effectsToBeTested)(Assertion.contains(EnrichedEvent(event1, "A", "B", 10_000)))
        .provide(TestClient.layer,
          ZLayer.fromFunction(() => new CounterpartyService {
            override def deanonymize[T](anonymizedCounterpartyId: CounterpartyHash, semaphore: Semaphore): ZIO[Client, Errors.ServiceCallError, String] =
              ZIO.succeed(anonymizedCounterpartyId.toUpperCase)
          }),
          ZLayer.fromFunction(() => new CounterpartyServiceConfig("", Schedule.recurs(0), 1)))

