package eu.karnicki.fun

import zio.StackTrace

object Errors:
  sealed trait ServiceCallError extends Throwable
  
  case object Transient extends ServiceCallError
  case object NotFound extends ServiceCallError
  case class ResponseError(code: String) extends ServiceCallError

  extension (s: (Throwable, StackTrace))
    def toErrorMessage = s match
      case (throwable, stackTrace) =>
        s"${throwable.getMessage}${System.lineSeparator}${stackTrace.stackTrace.take(5).mkString(System.lineSeparator)}"
