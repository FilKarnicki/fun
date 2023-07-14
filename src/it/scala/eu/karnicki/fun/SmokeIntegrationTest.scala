package eu.karnicki.fun

import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.{ContainerDef, ForEachTestContainer, GenericContainer}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

/**
 * For mac users, first run
 * sudo ln -s $HOME/.docker/run/docker.sock /var/run/docker.sock
 */
class SmokeIntegrationTest extends AnyFlatSpecLike with TestContainerForAll:
  override val containerDef: ContainerDef = GenericContainer.Def(
    "localstack/localstack:1.3.0",
    exposedPorts = Seq(80),
    waitStrategy = new LogMessageWaitStrategy().withRegEx(".*Ready\\.\n"))

  it should "start a docker container" in :
    withContainers: localstackContainer =>
      assert(1 == 1)