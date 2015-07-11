# fleet-client

An asynchronous [Scala](http://www.scala-lang.org/) client for the
[fleet](https://github.com/coreos/fleet) API based upon
[spray](http://spray.io/).

This client is supports all the operations listed under the official [fleet
API](https://github.com/coreos/fleet/blob/master/Documentation/api-v1.md).

## Usage

Below is a quick start guide for using the Scala fleet client.  For more
information, see the [comprehensive documentation for the Scala fleet
Client](http://monsantoco.github.io/fleet-client/).


```scala

implicit val system = ActorSystem("fleet")
implicit val ece = system.dispatcher

//define a new fleet configuration
//you can also specify headers to use in case you are accessing fleet through something like nginx for authentication
val config = FleetConfig(s"http://localhost:49153", List.empty[RawHeader])
val client = FleetClient(config)

val createUnitRequest = CreateUnitRequest(
  "myservice.service",
  Seq(UnitOption(Unit.UnitSection.UNIT, Unit.Unit.DESCRIPTION, "An empty service")),
  State.LAUNCHED
)

val response = client.createUnit(createUnitRequest)

response onComplete {
  case Success(_) =>
    System.out.println("Created unit!")
  case Failure(error) =>
    System.out.println("Failed to create unit: " + error)
}

val units = client.listUnits
units onSuccess { case u => u.foreach(println) }

system.shutdown()
```

## Testing

To successfully run the tests, you must have a fleet service running on IP
address 172.17.8.201 and listening on port 41337.  If you have Vagrant
installed, you can simply use the included `Vagrantfile` to start up a CoreOS
virtual machine with fleet running.

```
vagrant up
sbt test
```
