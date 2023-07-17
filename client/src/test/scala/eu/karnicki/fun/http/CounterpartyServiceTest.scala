package eu.karnicki.fun.http

import zio.*
import zio.http.{Client, Path, Request, Response, Status, TestClient, URL, ZClient}
import zio.test.*

object CounterpartyServiceTest extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment with Scope, Throwable] =
    suite("CounterpartyServiceSpec")(
      test("it should do something"):
        val request = Request.get(URL(Path.decode("/deanonymise/a")))
        val request2 = Request.get(URL(Path.decode("/deanonymise/z")))

        for {
          client <- ZIO.service[Client]
          _ <- TestClient.addRequestResponse(request, Response.ok)
          goodResponse <- client.request(request)
          badResponse <- client.request(request2)
        } yield assertTrue(goodResponse.status == Status.Ok) &&
          assertTrue(badResponse.status == Status.NotFound)
    ).provideLayer(TestClient.layer)