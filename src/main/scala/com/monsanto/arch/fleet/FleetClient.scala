package com.monsanto.arch.fleet

import akka.actor.ActorSystem
import spray.client.pipelining.{Get,Put,Delete}
import spray.httpx.SprayJsonSupport._
import scala.concurrent.Future

/**
 * Fleet client API.
 *
 * Abstract trait, so may be easily mocked out.
 *
 * See [[com.monsanto.arch.fleet]] for more usage examples.
 */
trait FleetClient {
  //unit methods
  /**
   * List all units
   * @return Future of Sequence of UnitEntities
   */
  def listUnits(): Future[Seq[UnitEntity]]

  /**
   * Get a specific unit for given name
   * @param name the unit name
   * @return the UnitEntity with the given name, or failure if not found
   */
  def getUnit(name: String): Future[UnitEntity]

  /**
   * Create a unit using [[com.monsanto.arch.fleet.CreateUnitRequest]]
   * @param request the create unit request
   * @return Future of status code returned from fleet
   */
  def createUnit(request: CreateUnitRequest): Future[FleetClient.StatusCode]

  /**
   * Attempt to update the given unit name to the desired state provided
   * @param name of unit to update
   * @param desiredState new desired state for unit
   * @return Future of status code returned from fleet
   */
  def modifyUnit(name: String, desiredState: String): Future[FleetClient.StatusCode]

  /**
   * Attempt to destroy a unit using the given name
   * @param name of unit to destroy
   * @return Future of status code from fleet
   */
  def destroyUnit(name: String): Future[FleetClient.StatusCode]

  /**
   * Get the unit states for all known units
   * @return Future sequence of unit states for all known units
   */
  def getStates(): Future[Seq[UnitState]]

  /**
   * List all known machine entities
   * @return Future sequence of all known machine entities
   */
  def listMachines(): Future[Seq[MachineEntity]]
}

object FleetClient {
  type StatusCode = Int

  /**
   * Obtain a FleetClient instance using a standard fleet implementation
   * @param config The fleet configuration to use
   * @param actorSystem implicit actor system must be provided for async
   * @return a FleetClient instance
   */
  def apply(config:FleetConfig)(implicit actorSystem:ActorSystem): FleetClient = new FleetClientImpl(config)

  private class FleetClientImpl(val config: FleetConfig)(implicit val system: ActorSystem)
      extends FleetClient with FleetPipelines {
    //unit methods
    def listUnits(): Future[Seq[UnitEntity]] =
      paginatedPipeline[ListUnitsResponse].apply(Get(config.URLs.units)).map(_.unitSeq)

    def getUnit(name: String): Future[UnitEntity] =
      pipeline[UnitEntity].apply(Get(config.URLs.unit(name)))

    def createUnit(request: CreateUnitRequest): Future[FleetClient.StatusCode] =
      noResponsePipeline(Put(config.URLs.unit(request.name), request))

    def modifyUnit(name: String, desiredState: String): Future[FleetClient.StatusCode] =
      noResponsePipeline(Put(config.URLs.unit(name), ModifyUnitRequest(desiredState)))

    def destroyUnit(name: String): Future[FleetClient.StatusCode] =
      noResponsePipeline(Delete(config.URLs.unit(name)))


    //state methods
    def getStates(): Future[Seq[UnitState]] =
      paginatedPipeline[StateResponse].apply(Get(config.URLs.state)).map(_.stateSeq)


    //machine methods
    def listMachines(): Future[Seq[MachineEntity]] =
      paginatedPipeline[ListMachinesResponse].apply(Get(config.URLs.machines)).map(_.machineSeq)
  }

}
