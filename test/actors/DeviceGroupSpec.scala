package actors

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike
import scala.concurrent.duration._

class DeviceGroupSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  import DeviceGroup._
  import DeviceManager.{DeviceRegistered, RequestTrackDevice}
  import Device.{RecordTemperature, TemperatureRecorded}

  "DeviceGroup actor" must {
    "be able to register a device actor" in {
      val probe = createTestProbe[DeviceRegistered]()
      val groupActor = spawn(DeviceGroup("group"))

      // register a device
      groupActor ! RequestTrackDevice("group", "device1", probe.ref)
      val registered1 = probe.receiveMessage()
      val deviceActor1 = registered1.device

      // register another device
      groupActor ! RequestTrackDevice("group", "device2", probe.ref)
      val registered2 = probe.receiveMessage()
      val deviceActor2 = registered2.device

      // IDs of registered deviced should be different
      deviceActor1 shouldNot equal(deviceActor2)

      // check that the devices are actually working
      val recordProbe = createTestProbe[TemperatureRecorded]()
      deviceActor1 ! RecordTemperature(requestId = 0, 1.0, recordProbe.ref)
      recordProbe.expectMessage(TemperatureRecorded(requestId = 0))
      deviceActor2 ! RecordTemperature(requestId = 1, 2.0, recordProbe.ref)
      recordProbe.expectMessage(TemperatureRecorded(requestId = 1))
    }

    "ignore requests for wrong groupId" in {
      val probe = createTestProbe[DeviceRegistered]()
      val groupActor = spawn(DeviceGroup("group"))

      groupActor ! RequestTrackDevice("wrongGroup", "device1", probe.ref)
      probe.expectNoMessage(500.milliseconds)
    }
  }
}
