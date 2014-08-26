package worker

import org.rogach.scallop.{ScallopOption, ScallopConf}
import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem, Address}

/**
 * @author carl
 *         2014-05-13 19:31
 */

// Need to know how many of each thing to start
// Need to know the roles for master(s)
// Need to know address created, given, config

trait Configuration {
  def startBackend(joinAddressOption: Option[Address], role: String): Address

  def startWorker(contactAddress: akka.actor.Address): Unit

  def startFrontend(joinAddress: akka.actor.Address): Unit

  def start(args: Array[String]): Unit = {

    object Conf extends ScallopConf(args) {
      val masters:ScallopOption[List[Int]] = opt[List[Int]](
        descr = "Number of masters",
        required = false,
        default = Option(List(1)),
        validate = (msl => msl.length <= 3 & msl.forall(_ >= 0)))

      val frontends:ScallopOption[Int] = opt[Int](
        descr = "Number of frontends",
        required = false,
        default = Option(1),
        validate = (_ >=0))

      val workers:ScallopOption[Int] = opt[Int](
        descr = "Number of workers",
        required = false,
        default = Option(2),
        validate = (_ >=0))
    }

    println(s"Number of master: ${Conf.masters}")
    println(s"Number of frontends: ${Conf.frontends}")
    println(s"Number of workers: ${Conf.workers}")

    val (masters, shards, firstShard) = Conf.masters.get match {
      case Some(m :: s :: f :: Nil) => (m, s, f)
      case Some(m :: s :: Nil) => (m, s, 1)
      case Some(m :: Nil) => (m, 1, 1)
      case Some(Nil) => (0, 0, 0)
      case None => (0, 0, 0)
      case x => throw new IllegalArgumentException("Not a valid master param")
    }

    //for (s <- 1 to shards; m <- 1 to masters) startBackend(Some(joinAddress), s"backend-shard$shards")
    val msx = for (s <- firstShard until (firstShard + shards); m <- 1 to masters) yield (m, s)

    val joinAddress = msx.foldLeft (None: Option[Address]) {
      (joinAddress, ms) =>
        val (_, shard) = ms
        val role = s"backend-shard-$shard"
        val address = startBackend(joinAddress, role)
        Thread.sleep(5000)
        Some(address)
    }.get

    for (f <- 1 to Conf.frontends.get.getOrElse(0)) startFrontend(joinAddress)

    for (w <- 1 to Conf.workers.get.getOrElse(0)) startWorker(joinAddress)

  }


}

object MainConfigurable extends Startup with Configuration {

  def main(args: Array[String]): Unit = {

    start(args)

  }
}

/*
trait StartableComponent {
  val startable: StartableDevice
  trait StartableDevice {
    def startup(joinAddress: Address): Unit
  }

}

trait ConfigurableComponent {
  val configurable: ConfigurableDevice
  trait ConfigurableDevice {
    def numToStart: Int
    //def system: ActorSystem
    def joinAddress: Address
  }
}

trait StartableComponentImpl extends StartableComponent {
  class WorkerStarter extends StartableDevice {
    def startup(joinAddress: Address) = println(s"Starting Worker with joinAddress: $joinAddress")
  }
}

trait ConfigurableComponentImpl extends ConfigurableComponent {
  class WorkerConfiger extends ConfigurableDevice {
    def numToStart = 4
    def joinAddress = akka.actor.Address(system = "foo", protocol = "akka.tcp", host = "localhost", port = 0)
  }
}

trait WorkerConfigerStarterImpl {
  this: StartableComponentImpl with ConfigurableComponentImpl =>
  class Worker {
    def start = {
      for (w <- 1 to configurable.numToStart) startable.startup(configurable.joinAddress)
    }
  }
}

object ComponentRegistry extends
StartableComponentImpl with
ConfigurableComponentImpl with
WorkerConfigerStarterImpl {

  val startable = new WorkerStarter
  val configurable = new WorkerConfiger
  val worker = new Worker
}


object CakeMain {
  def main(args: Array[String]){

    val worker = ComponentRegistry.worker
    worker.start

  }
}

*/