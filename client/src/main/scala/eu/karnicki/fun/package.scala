package eu.karnicki

import zio.ZIO

package object fun:
  extension[R, E, A] (z: ZIO[R, E, A])
    def debugThread: ZIO[R, E, A] =
      z.tap(a => ZIO.succeed(println(s"[${Thread.currentThread.getName}] $a")))
        .tapErrorCause(cause => ZIO.succeed(println(s"[${Thread.currentThread.getName}][FAILURE] $cause")))

  enum ClientSide:
    case Buyer, Seller

  enum Permissions {
    case READ, WRITE, EXEC
  }
