package eu.karnicki.fun

import cats.*
import cats.data.Reader
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import com.comcast.ip4s.{Ipv4Address, Port, ipv4, port}
import eu.karnicki.fun.KindsAndTypeLambdas.EitherMonad
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.server.*
import zio.schema.validation.Validation

import scala.util.{Failure, Success, Try}

object CounterpartyServiceApp extends IOApp:
  private val counterpartyStore: Map[String, String] =
    Map(
      "a" -> "CPTY_ID_1",
      "b" -> "CPTY_ID_2")

  @main def main(args: String*): Unit =
    println("server")

  def rootRoute[F[_] : Monad]: HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root =>
        Ok("Oh hi there!")
    }

  // GET /deanonymize/$counterpartyHash
  def counterpartyRoutes[F[_] : Monad]: HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "deanonymize" / counterpartyHash =>
        Thread.sleep(1000)
        counterpartyStore.get(counterpartyHash) match
          case Some(counterpartyId) if counterpartyId.isBlank =>
            BadRequest(counterpartyHash)
          case Some(counterpartyId) =>
            Ok(counterpartyId)
          case None =>
            NotFound(counterpartyHash)
    }

  def allRoutes[F[_] : Monad]: HttpRoutes[F] =
    rootRoute[F] <+> counterpartyRoutes[F]

  def allRoutesComplete[F[_] : Monad]: HttpApp[F] =
    allRoutes[F].orNotFound

  case class Config(host: Ipv4Address, port: Option[Port])

  private val configReader: Reader[List[String], Config] = Reader(args =>
    Config(
      ipv4"0.0.0.0",
      args
        .headOption
        .flatMap(head =>
          scala.util.Try(head.toInt).toOption)
        .flatMap(Port.fromInt)))

  override def run(args: List[String]): IO[ExitCode] =
    val (host, port) = (for {
      host <- configReader.map(_.host)
      maybePort <- configReader.map(_.port)
      port =  maybePort.getOrElse(Port.fromInt(8080).get)
    } yield (host, port)).run(args)

    EmberServerBuilder
      .default[IO]
      .withHost(host)
      .withPort(port)
      .withHttpApp(allRoutesComplete)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
