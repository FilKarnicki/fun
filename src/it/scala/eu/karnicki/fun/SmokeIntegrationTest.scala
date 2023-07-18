package eu.karnicki.fun

import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.{ContainerDef, ForEachTestContainer, GenericContainer}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers.shouldBe
import org.testcontainers.containers.wait.strategy.{HttpWaitStrategy, LogMessageWaitStrategy}
import zio.Unsafe

import scala.sys.process.ProcessIO
import scala.util.Try

/**
 * For mac users, first run
 * sudo ln -s $HOME/.docker/run/docker.sock /var/run/docker.sock
 */

/**
 * First run sbt Docker /publishLocal
 * on server
 */
class SmokeIntegrationTest extends AnyFlatSpecLike with TestContainerForAll:
  override val containerDef: ServerContainer.Def = ServerContainer.Def(8080)

  it should "start a docker container" in {
    withContainers {
      serverContainer =>
        ClientApp.main(Array("8080")) // TODO: NO BUENO!
        serverContainer.underlyingUnsafeContainer.getLogs.contains("EnrichedEvent") shouldBe true
    }
  }