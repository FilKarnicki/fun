package eu.karnicki.fun.http

import eu.karnicki.fun.http.CounterpartyService.CounterpartyServiceLive
import zio.*
import zio.http.*
import zio.test.*

object CounterpartyServiceTest extends ZIOSpecDefault:
  private lazy val configZLayer: ULayer[CounterpartyServiceConfig] =
    ZLayer.succeed(CounterpartyServiceConfig("http://localhost/deanonymise/", Schedule.recurs(0)))

  private lazy val testServiceLayer: ZLayer[CounterpartyServiceConfig, Nothing, CounterpartyService] = ZLayer.scoped {
    for {
      testConfig <- ZIO.service[CounterpartyServiceConfig]
    } yield CounterpartyServiceLive(testConfig)
  }

  override def spec =
    suite("CounterpartyServiceSpec")(
      test("it should do something"):
        val request = Request.get(URL(Path.decode("/deanonymise/a")))
        val request2 = Request.get(URL(Path.decode("/deanonymise/z")))

        for {
          client <- ZIO.service[Client]
          _ <- TestClient.addRequestResponse(request, Response(status = Status.Ok, body = Body.fromString("A")))
          _ <- TestClient.addRequestResponse(request2, Response(status = Status.NotFound, body = Body.fromString("z")))
          goodResponse <- client.request(request)
          badResponse <- client.request(request2)

          counterpartyService <- ZIO.service[CounterpartyService]
          responseA <- counterpartyService.deanonymize("a")
          responseZ <- counterpartyService.deanonymize("z")
        } yield assertTrue(goodResponse.status == Status.Ok) &&
          assertTrue(badResponse.status == Status.NotFound) &&
          assertTrue(responseA == "A") &&
          assertTrue(responseZ == "z")
    )
      .provide(TestClient.layer,
        configZLayer,
        testServiceLayer)