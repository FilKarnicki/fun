package eu.karnicki.fun.http

import eu.karnicki.fun.Errors
import eu.karnicki.fun.Errors.{ResponseError, ServiceCallError, Transient}
import eu.karnicki.fun.http.CounterpartyService.CounterpartyServiceLive
import zio.*
import zio.Cause.Die
import zio.Exit.Failure
import zio.http.*
import zio.test.*
import zio.test.Assertion.*

import java.net.ConnectException

object CounterpartyServiceTest extends ZIOSpecDefault:
  private lazy val configZLayer: ULayer[CounterpartyServiceConfig] =
    ZLayer.succeed(
      CounterpartyServiceConfig("http://localhost/deanonymise/",
      Schedule.recurs(3),
      maxInFlight = 1))

  private lazy val testServiceLayer: ZLayer[CounterpartyServiceConfig, Nothing, CounterpartyService] = ZLayer.scoped {
    for {
      testConfig <- ZIO.service[CounterpartyServiceConfig]
    } yield CounterpartyServiceLive(testConfig)
  }

  override def spec =
    suite("CounterpartyServiceSpec")(
      test("it should map 200 to string and fail with NotFound if 404"):
        val request = Request.get(URL(Path.decode("/deanonymise/a")))
        val request2 = Request.get(URL(Path.decode("/deanonymise/z")))
        var requestCalls = 0
        for {
          semaphore <- Semaphore.make(10)
          client <- ZIO.service[Client]
          _ <- TestClient.addHandler {
            case request if request.path.last.contains("a") =>
              ZIO.succeed(Response(status = Status.Ok, body = Body.fromString("A")))
            case request: Request if requestCalls > 3 =>
              ZIO.succeed(Response(status = Status.Ok, body = Body.fromString("Z")))
            case request: Request =>
              requestCalls = requestCalls + 1 // TODO: ugly
              ZIO.succeed(Response(status = Status.NotFound, body = Body.fromString("z")))
          }
          counterpartyService <- ZIO.service[CounterpartyService]
          responseA <- counterpartyService.deanonymize("a",semaphore)
          responseZ <- counterpartyService.deanonymize("z",semaphore).exit
        } yield assertTrue(responseA == "A") &&
          assertTrue(responseZ == Failure(Cause.fail(Errors.NotFound)))
    )
      .provide(TestClient.layer,
        configZLayer,
        testServiceLayer)