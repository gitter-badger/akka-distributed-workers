package worker

import akka.actor.{ActorSystem, Address, Props, RootActorPath}
import akka.contrib.pattern.ClusterClient
import worker._

/**
 * @author carl
 *         2014-05-16 19:25
 */

/*

 how many to startup


 startup
 =======
 Need joinAddress

 Need systemName
 Need system (Could be different system for each top level actor master/worker/frontend or shared for all or shared across types

 Need worktimeout - Master
 Need role - master

  */

// =======================
// service interfaces
// components
trait StarterComponent {
  def starter: StarterDevice

  trait StarterDevice {
    def startup(systemName: String, joinAddress: Address): Unit
  }
}

trait ConfigurationComponent {
  def configuration: ConfigurationDevice

  trait ConfigurationDevice {
    def systemName: String
    def numToStart: Int

    //def system: ActorSystem
    def joinAddress: Address
  }
}

// =======================
// service implementations
// implementations
trait WorkerStarterComponent extends StarterComponent {
  def starter = new WorkerStarter

  class WorkerStarter extends StarterDevice {
    def startup(systemName: String, contactAddress: Address) {
      println(s"Starting Worker with joinAddress: $contactAddress")

      val system = ActorSystem(systemName)
      val initialContacts = Set(
        system.actorSelection(RootActorPath(contactAddress) / "user" / "receptionist"))
      val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
      system.actorOf(Worker.props(clusterClient, Props[WorkExecutor]), "worker")
    }
  }
}

trait WorkerConfigurationComponent extends ConfigurationComponent {
  def configuration = new WorkerConfiguration

  class WorkerConfiguration extends ConfigurationDevice {
    def systemName = "MasterWorker"
    def numToStart = 2
    def joinAddress = akka.actor.Address(system = "foo", protocol = "akka.tcp", host = "localhost", port = 0)
  }
}

// =======================
// service declaring two dependencies that it wants injected
// machine
trait WorkerConfigerStarterImpl {
  self: StarterComponent with ConfigurationComponent =>
  //  class Worker {
  //    def start = {
  //      for (w <- 1 to configuration.numToStart)
  //        starter.startup(configuration.systemName, configuration.joinAddress)
  //    }
  //  }

  // alternative
  def start = {
    for (w <- 1 to configuration.numToStart)
      starter.startup(configuration.systemName, configuration.joinAddress)
  }
}

// =======================
// instantiate the services in a module
// top level object
object ComponentRegistry extends
WorkerStarterComponent with
WorkerConfigurationComponent with
WorkerConfigerStarterImpl {

  //val startable = new WorkerStarter
  //val configurable = new WorkerConfiger
  //val worker = new Worker
}

// =======================
object MainCake {

  def main(args: Array[String]) {
    //val worker = ComponentRegistry.worker
    //worker.start

    ComponentRegistry.start
  }

}


// object DeluxExpressoMachine extends ExpressoMachine with FineGrinderComponent with AutomaticFoamerComponent with FastWaterHeaterComponent
// object BudgetExpressonMachine extends ExpressoMachine with RoughGrinderComponent with ManualFoamerComponent with SlowWaterHeaterComponent

//object ScallopConfiguredWorker extends ConfiguredWoker with

