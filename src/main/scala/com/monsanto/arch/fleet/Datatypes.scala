package com.monsanto.arch.fleet

import spray.json._
import FleetPipelines.Groupinator

/**
 * Value class for unit creation request
 * @param name desired name for unit
 * @param options sequence of unit options
 * @param desiredState state to create unit in
 *
 * Example usage:
 * {{{
 * val request = CreateUnitRequest("frederick.service",
 *   Seq(UnitOption(Unit.UnitSection.UNIT, Unit.Unit.DESCRIPTION, "An empty service")),
 *   State.LAUNCHED)
 * }}}
 */
case class CreateUnitRequest(name:           String,
                             options:        Seq[UnitOption],
                             desiredState:   String)
object CreateUnitRequest {
  import DefaultJsonProtocol._
  implicit val format: RootJsonFormat[CreateUnitRequest] = jsonFormat3(CreateUnitRequest.apply)
}

/**
 * Holder for response to ListMachines request
 * @param machines Optional sequence of MachineEntity values
 */
case class ListMachinesResponse(machines: Option[Seq[MachineEntity]]) {
  def machineSeq = machines.getOrElse(Nil)
}
object ListMachinesResponse {
  import DefaultJsonProtocol._

  implicit val groupinator:Groupinator[ListMachinesResponse] = new Groupinator[ListMachinesResponse] {
    override def add(a: ListMachinesResponse, b: ListMachinesResponse): ListMachinesResponse = ListMachinesResponse(Some(a.machineSeq ++ b.machineSeq))
  }

  implicit val format: RootJsonFormat[ListMachinesResponse] = jsonFormat1(ListMachinesResponse.apply)
}


/**
 * Holder for response to ListUnits request
 * @param units optional sequence of UnitEntity values
 */
case class ListUnitsResponse(units: Option[Seq[UnitEntity]]) {
  def unitSeq = units.getOrElse(Nil)
}
object ListUnitsResponse {
  import DefaultJsonProtocol._

  implicit val jsonFormat: RootJsonFormat[ListUnitsResponse] = jsonFormat1(ListUnitsResponse.apply)

  implicit val groupinator:Groupinator[ListUnitsResponse] = new Groupinator[ListUnitsResponse] {
    override def add(a: ListUnitsResponse, b: ListUnitsResponse): ListUnitsResponse =
      ListUnitsResponse(Some(a.unitSeq ++ b.unitSeq))
  }
}

/**
 * Value class for request to modify the state of a unit
 * @param desiredState the new desired state for the unit
 */
case class ModifyUnitRequest(desiredState: String)
object ModifyUnitRequest {
  import DefaultJsonProtocol._
  implicit val format: RootJsonFormat[ModifyUnitRequest] = jsonFormat1(ModifyUnitRequest.apply)
}

/**
 * Value class representing a machine entity
 * @param id the uuid of the machine
 * @param metadata dictionary of key-value data published by the machine
 * @param primaryIP primary IP address for communication
 */
case class MachineEntity(id:         String,
                         metadata:   Map[String, String],
                         primaryIP:  String)
object MachineEntity {
  import DefaultJsonProtocol._
  implicit val format: RootJsonFormat[MachineEntity] = jsonFormat3(MachineEntity.apply)
}

/**
 * Value class representing a Unit state
 * @param name unit name
 * @param hash SHA1 hash of underlying unit file
 * @param machineID optional assigned machine ID. Optional
 *                  because it may not have been assigned yet
 *                  by fleet.
 * @param systemdLoadState load state from systemd
 * @param systemdActiveState active state from systemd
 * @param systemdSubState sub state from systemd
 */
case class UnitState(name:                 String,
                     hash:                 String,
                     machineID:            Option[String],
                     systemdLoadState:     String,
                     systemdActiveState:   String,
                     systemdSubState:      String)
object UnitState {
  import DefaultJsonProtocol._
  implicit val jsonFormat:RootJsonFormat[UnitState] = jsonFormat6(UnitState.apply)
}

/**
 * Value class representing a Unit entity
 * @param name unit name
 * @param options sequence of UnitOption entities
 * @param desiredState state the user wishes the entity to be in
 * @param currentState state the entity is actually in
 * @param machineID optional machine ID. Optional because
 *                  fleet may not have assigned it yet.
 */
case class UnitEntity(name:           String,
                      options:        Seq[UnitOption],
                      desiredState:   String,
                      currentState:   String,
                      machineID:      Option[String])
object UnitEntity {
  import DefaultJsonProtocol._
  implicit val format: RootJsonFormat[UnitEntity] = jsonFormat5(UnitEntity.apply)
}

/**
 * State definitions for current/desired state
 */
object State {
  val LAUNCHED  = "launched"
  val LOADED    = "loaded"
  val INACTIVE  = "inactive"
}

/**
 * systemd reported state definitions
 */
object SystemdStates {
  val INACTIVE    = "inactive"
  val ACTIVE      = "active"
  val ACTIVATING  = "activating"
}

/**
 * systemd reported sub-state definitions
 */
object SystemdSubStates {
  val DEAD          = "dead"
  val WAITING       = "waiting"
  val RUNNING       = "running"
  val AUTO_RESTART  = "auto-restart"
  val EXITED        = "exited"
}

/**
 * Unit option definition
 * @param section name of section that contains the option
 *                (e.g. "Unit", "Service", "Socket")
 * @param name name of option (e.g. "BindsTo", "After", "ExecStart")
 * @param value value of option (e.g.
 *              "/usr/bin/docker run busybox /bin/sleep 1000")
 */
case class UnitOption(section:    String,
                      name:       String,
                      value:      String)
object UnitOption {
  import DefaultJsonProtocol._
  implicit val format: RootJsonFormat[UnitOption] = jsonFormat3(UnitOption.apply)
}

/**
 * A collection of useful constant definitions
 */
object Unit {
  object UnitSection {
    val UNIT                = "Unit"
    val SERVICE             = "Service"
    val SOCKET              = "Socket"
    val FLEET               = "X-Fleet"
  }

  // a full list of available options is at http://www.freedesktop.org/software/systemd/man/systemd.unit.html
  object Unit {
    val DESCRIPTION         = "Description"
    val REQUIRES            = "Requires"
    val AFTER               = "After"
    val BEFORE              = "Before"
    val WANTS               = "Wants"
    val CONFLICTS           = "Conflicts"
    val ON_FAILURE          = "OnFailure"
  }

  // a full list of available options is at http://www.freedesktop.org/software/systemd/man/systemd.service.html
  object Service {
    val TYPE                = "Type"
    val REMAIN_AFTER_EXIT   = "RemainAfterExit"

    val TIMEOUT_START_SEC   = "TimeoutStartSec"
    val TIMEOUT_SEC         = "TimeoutSec"
    val TIMEOUT_STOP_SEC    = "TimeoutStopSec"

    val RESTART             = "Restart"
    val RESTART_SEC         = "RestartSec"

    val EXEC_START_PRE      = "ExecStartPre"
    val EXEC_START          = "ExecStart"
    val EXEC_START_POST     = "ExecStartPost"

    val EXEC_RELOAD         = "ExecReload"

    val EXEC_STOP_PRE       = "ExecStopPre"
    val EXEC_STOP           = "ExecStop"
    val EXEC_STOP_POST      = "ExecStopPost"
  }

  object Fleet {
    val MACHINE_ID          = "MachineID"
    val MACHINE_OF          = "MachineOf"
    val MACHINE_METADATA    = "MachineMetadata"
    val CONFLICTS           = "Conflicts"
    val GLOBAL              = "Global"
  }
}

/**
 * Response to a state request
 * @param states optional sequence of UnitStates
 */
case class StateResponse(states: Option[Seq[UnitState]]) {
  def stateSeq = states.getOrElse(Nil)
}

object StateResponse {
  import DefaultJsonProtocol._

  implicit val jsonFormat: RootJsonFormat[StateResponse] = jsonFormat1(StateResponse.apply)

  implicit val groupinator: Groupinator[StateResponse] = new Groupinator[StateResponse] {
    override def add(a: StateResponse, b: StateResponse): StateResponse = StateResponse(Some(a.stateSeq ++ b.stateSeq))
  }
}
