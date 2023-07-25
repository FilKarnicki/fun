package eu.karnicki.fun.http

import eu.karnicki.fun.Errors
import eu.karnicki.fun.Errors.{ServiceCallError, Transient}
import eu.karnicki.fun.http.CounterpartyService.CounterpartyServiceLive
import zio.*
import zio.Cause.Die
import zio.http.*
import zio.test.*
import zio.test.Assertion.*

import java.net.ConnectException

object CounterpartyServiceTest extends ZIOSpecDefault:
  private lazy val configZLayer: ULayer[CounterpartyServiceConfig] =
    ZLayer.succeed(CounterpartyServiceConfig("http://localhost/deanonymise/", Schedule.recurs(10)))

  private lazy val testServiceLayer: ZLayer[CounterpartyServiceConfig, Nothing, CounterpartyService] = ZLayer.scoped {
    for {
      testConfig <- ZIO.service[CounterpartyServiceConfig]
    } yield CounterpartyServiceLive(testConfig)
  }

  override def spec =
    suite("CounterpartyServiceSpec")(
      test("it should return resolved value if available and non-resolved one if not"):
        val request = Request.get(URL(Path.decode("/deanonymise/a")))
        val request2 = Request.get(URL(Path.decode("/deanonymise/z")))
        var requestCalls = 0
        for {
          client <- ZIO.service[Client]
          _ <- TestClient.addHandler {
            case request if request.path.last.contains("a") =>
              ZIO.succeed(Response(status = Status.Ok, body = Body.fromString("A")))
            case request: Request if requestCalls > 3 =>
              ZIO.succeed(Response(status = Status.Ok, body = Body.fromString("Z")))
            case request: Request  =>
              requestCalls = requestCalls + 1 // TODO: ugly
              ZIO.failCause(Cause.fail(new ConnectException("oh noes"))) // TODO: doesn't actually cause the client to fail
          }
          counterpartyService <- ZIO.service[CounterpartyService]
          responseA <- counterpartyService.deanonymize("a")
          responseZ <- counterpartyService.deanonymize("z").exit
        } yield assertTrue(responseA == "A") &&
          assert(responseZ)(fails(equalTo(Transient))) // && TODO: assert


    )
      .provide(TestClient.layer,
        configZLayer,
        testServiceLayer)