package com.monsanto.arch

/**
 * A lightweight fleet client wrapper for Scala.
 *
 * Sample usage:
 *
 * {{{
 *   implicit val system = ActorSystem("fleet-ahoy")
 *   implicit val ece = system.dispatcher
 *
 *   val config = FleetConfig("http://172.17.8.201:49153", Nil)
 *   val client = FleetClient(config)  // obtain a fleet client instance
 *
 *   // of course, one should not normally Await or otherwise block in a
 *   // well behaved system
 *   val listUnits = client.listUnits()  // gives back a Future
 *   units onSuccess { case u => u foreach println }
 * }}}
 *
 * Fleet Client is an asynchronous API and returns Future results for all calls.
 * It is tempting to block and await these results, but a recommended
 * and more effective way to use them is to compose them together, often
 * using a for expression. An example usage is given in this test (which
 * can be seen in the code).
 *
 * {{{
 *   it("should compose nicely in a for expression") {
 *     implicit val patienceConfig = PatienceConfig(10.seconds, 100.milliseconds)
 *
 *     val newUnitRequest = CreateUnitRequest("frederick.service",
 *       Seq(UnitOption(Unit.UnitSection.UNIT, Unit.Unit.DESCRIPTION, "An empty service")),
 *       State.LAUNCHED)
 *
 *     val createFrederick = for {
 *       units    <- client.listUnits() if !units.exists(_.name == "frederick.service")
 *       code     <- client.createUnit(newUnitRequest)
 *       unitsNow <- client.listUnits()
 *     } yield {
 *       code should be(201)
 *       units.exists(_.name == "frederick.service") should be (false)
 *       unitsNow.exists(_.name == "frederick.service") should be (true)
 *     }
 *
 *     val removeFrederick = for {
 *       _        <- createFrederick   // dependent on completion of createFrederick
 *       removed  <- client.destroyUnit("frederick.service")
 *       units    <- client.listUnits()
 *     } yield {
 *       removed should be (204)
 *       units.exists(_.name == "frederick.service") should be (false)
 *     }
 *
 *     eventually {
 *       removeFrederick.isCompleted should be (true)
 *     }
 *     removeFrederick.value should be (Some(Success(())))
 *   }
 *
 * }}}
 *
 * Note that the `eventually` does await resolution of the future (something has
 * to when testing) but up until that point the entire operation is
 * non blocking, with a future being passed forward through all of the
 * operations.
 *
 * The second remove operation even depends on the first
 * create operation without blocking, by using createFrederick as the
 * first operation in the for block: `_ <- createFrederick`.
 *
 * For questions about specifics in this API, you will often find it
 * useful to refer to the
 * [[https://github.com/coreos/fleet/blob/master/Documentation/api-v1.md
 * underlying ReST API documentation]]
 */
package object fleet {

}
