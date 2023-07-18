package eu.karnicki.fun

import com.dimafeng.testcontainers.GenericContainer
import eu.karnicki.fun.ServerContainer.localPort
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy

import scala.jdk.CollectionConverters.*

class ServerContainer private(hostPort: Int, underlying: GenericContainer) extends GenericContainer(underlying) {
  underlying.container.setPortBindings(
    List(s"$hostPort:$localPort").asJava
  )

  val port: Int = hostPort
}

object ServerContainer {
  private val localPort = 8080

  case class Def(hostPort: Int)
    extends GenericContainer.Def[ServerContainer](
      new ServerContainer(
        hostPort,
        GenericContainer(
          dockerImage = "local/fun:0.1",
          exposedPorts = Seq(localPort),
          waitStrategy = new HttpWaitStrategy().forPort(localPort)
        )
      )
    )
}