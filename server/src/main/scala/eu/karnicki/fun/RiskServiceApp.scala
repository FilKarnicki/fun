package eu.karnicki.fun

import eu.karnicki.fun.RiskCalc.calcRisk
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import zio.*
import zio.http.*
import zio.http.Status.{BadRequest, InternalServerError}

import scala.util.Try

object RiskServiceApp extends ZIOAppDefault:
  implicit val statusEncoder: Encoder[eu.karnicki.fun.Status] = Encoder[String].contramap(_.toString)
  implicit val statusDecoder: Decoder[eu.karnicki.fun.Status] = Decoder[String].emap(string =>
    Try(eu.karnicki.fun.Status.valueOf(string)).toOption.toRight(s"Invalid status: $string. Try ${eu.karnicki.fun.Status.values.asJson}"))

  val app: App[Any] =
    Http.collectZIO[Request]:
      case req@Method.POST -> Root / "risk" =>
        req.body.asString.map { string =>
          val parsedObligation: Either[Error, Obligation] = decode[Obligation](string)
          parsedObligation
        }.map {
          case Left(error) =>
            Response.text(s"could not deserialize obligation due to ${error.getMessage}").withStatus(BadRequest)
          case Right(obligation) =>
            Response.text(calcRisk(obligation.instruments).asJson.toString)
        }.refineOrDie {
          case t: Throwable =>
            Response.text(s"unknown error: ${t.getMessage}").withStatus(InternalServerError)
        }
      case Method.GET -> Root / "risk" => ZIO.succeed(Response.text("Send an obligation to the POST endpoint"))

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    Server
      .serve(app)
      .provide(
        ZLayer.succeed(Server.Config.default.port(8081)),
        Server.live)
