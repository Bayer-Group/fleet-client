package com.monsanto.arch.fleet

import java.util.UUID
import akka.actor.ActorSystem
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import spray.http.StatusCodes
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Success

class Fleet_DBUT extends FunSpec with Matchers with Eventually with BeforeAndAfter {
  implicit val system = ActorSystem("dbut")
  implicit val ece = system.dispatcher

  val ip = "172.17.8.201"
  val port = 41337
  val config = FleetConfig(s"http://$ip:$port", List.empty)
  val client = FleetClient(config)

  val timeout = 10.seconds

  def createUnit = CreateUnitRequest(
    UUID.randomUUID().toString + ".service",
    Seq(UnitOption(Unit.UnitSection.UNIT, Unit.Unit.DESCRIPTION, "An empty service")),
    State.LAUNCHED
  )

  def createNamedUnit(name: String) = CreateUnitRequest(
    name,
    Seq(UnitOption(Unit.UnitSection.UNIT, Unit.Unit.DESCRIPTION, "An empty service")),
    State.LAUNCHED
  )

  val testUnit = createUnit

  def cleanup() {
    val listUnits = Await.result(client.listUnits(), timeout)
    listUnits.foreach(u => Await.result(client.destroyUnit(u.name), timeout))
  }

  before { cleanup() }

  describe("Fleet") {
    it("should return back a machine") {
      val response = Await.result(client.listMachines(), timeout)
      assert(response.length === 1)

      val machine = response.head

      assert(machine.primaryIP === ip)
      assert(machine.metadata  === Map("hostname" -> "core-1"))
    }

    it("should list clients in a for expression") {
      val unitsTest = for {
        units <- client.listUnits()
      } yield {
        units should be (empty)
      }

      implicit val patienceConfig = PatienceConfig(30.seconds, 100.milliseconds)

      eventually {
        unitsTest.isCompleted should be (true)
      }
      unitsTest.value should be (Some(Success(())))
    }

    it("should allow creation of a unit") {
      val uuid = UUID.randomUUID()
      val unitResult = Await.result(client.createUnit(
        createNamedUnit(uuid.toString + ".service")), 10.seconds)

      // now list the units
      val units = Await.result(client.listUnits(), 10.seconds)
      units should have size (1)
      units.head.name should be (s"$uuid.service")
    }

    //we can't test the empty case here since there will always be at least one machine

    it("should compose nicely in a for expression") {
      implicit val patienceConfig = PatienceConfig(10.seconds, 100.milliseconds)

      val newUnitRequest = CreateUnitRequest("frederick.service",
        Seq(UnitOption(Unit.UnitSection.UNIT, Unit.Unit.DESCRIPTION, "An empty service")),
        State.LAUNCHED)

      val createFrederick = for {
        units    <- client.listUnits() if !units.exists(_.name == "frederick.service")
        code     <- client.createUnit(newUnitRequest)
        unitsNow <- client.listUnits()
      } yield {
        code should be(201)
        units.exists(_.name == "frederick.service") should be (false)
        unitsNow.exists(_.name == "frederick.service") should be (true)
      }

      val removeFrederick = for {
        _        <- createFrederick   // dependent on completion of createFrederick
        removed  <- client.destroyUnit("frederick.service")
        units    <- client.listUnits()
      } yield {
        removed should be (204)
        units.exists(_.name == "frederick.service") should be (false)
      }

      eventually {
        removeFrederick.isCompleted should be (true)
      }
      removeFrederick.value should be (Some(Success(())))
    }

    it("should allow a newly created unit to have state modified") {
      val newUnitRequest = CreateUnitRequest("statecheck.service",
        Seq(UnitOption(Unit.UnitSection.UNIT, Unit.Unit.DESCRIPTION, "An empty service")),
        State.LOADED)

      val updateState = for {
        created     <- client.createUnit(newUnitRequest)
        firstState  <- client.getUnit("statecheck.service")
        updated     <- client.modifyUnit("statecheck.service", State.LAUNCHED)
        secondState <- client.getUnit("statecheck.service")
      } yield {
        created should be (201)
        firstState.desiredState should be (State.LOADED)
        updated should be (204)
        secondState.desiredState should be (State.LAUNCHED)
      }

      eventually {
        updateState.isCompleted should be (true)
      }

      updateState.value should be (Some(Success(())))
    }

    it("should create/list/modify/delete a unit") {
      val createResponse = Await.result(client.createUnit(testUnit), timeout)
      assert(createResponse === StatusCodes.Created.intValue)

      implicit val patienceConfig = PatienceConfig(10.seconds, 100.milliseconds)

      eventually {
        val fetchedUnit = Await.result(client.getUnit(testUnit.name), timeout)
        assert(fetchedUnit.name == testUnit.name)
        assert(fetchedUnit.desiredState == State.LAUNCHED)
      }

      //make a lot of containers to make sure pagination is working
      val containers = (1 to 200).map{ _ =>
        val unit = createUnit
        Await.result(client.createUnit(unit), timeout)
        unit
      }

      eventually {
        val listUnits = Await.result(client.listUnits(), timeout)
        val unitNames = listUnits.map(_.name)
        assert(unitNames.contains(testUnit.name))
        containers.foreach(c => assert(unitNames.contains(c.name)))
      }

      eventually {
        val state = Await.result(client.getStates(), timeout)
        val stateNames = state.map(_.name)
        assert(stateNames.contains(testUnit.name))
        containers.foreach(c => assert(stateNames.contains(c.name)))
      }

      val modifyUnit = Await.result(client.modifyUnit(testUnit.name, State.INACTIVE), timeout)
      assert(modifyUnit === StatusCodes.NoContent.intValue)

      eventually {
        val fetchedUnit = Await.result(client.getUnit(testUnit.name), timeout)
        assert(fetchedUnit.name == testUnit.name)
        assert(fetchedUnit.desiredState == State.INACTIVE)
      }

      val deleteResponse = Await.result(client.destroyUnit(testUnit.name), timeout)
      assert(deleteResponse === StatusCodes.NoContent.intValue)

      containers.foreach { c => Await.result(client.destroyUnit(c.name), timeout) }

      eventually {
        val error = intercept[Exception] {
          Await.result(client.getUnit(testUnit.name), timeout)
        }
        assert(error.toString === "spray.httpx.UnsuccessfulResponseException: Status: 404 Not Found\nBody: {\"error\":{\"code\":404,\"message\":\"unit does not exist\"}}")
      }
    }
  }

  after { cleanup() }

}
