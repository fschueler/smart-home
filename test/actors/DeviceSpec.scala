package actors

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike

class DeviceSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  import Device._

  "Device actor" must {
    "reply with empty reading if no temperature is known" in {
      val probe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(Device("group", "device"))

      deviceActor ! Device.ReadTemperature(requestId = 42, probe.ref)
      val response = probe.receiveMessage()
      response.requestId shouldBe 42
      response.value shouldBe None
    }

    "reply with latest temperature reading" in {
      val recordProbe = createTestProbe[TemperatureRecorded]()
      val readProbe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(Device("group", "device"))

      deviceActor ! Device.RecordTemperature(requestId = 1, 24.0, recordProbe.ref)
      recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 1))

      deviceActor ! Device.ReadTemperature(requestId = 2, readProbe.ref)
      val response1 = readProbe.receiveMessage()
      response1.requestId shouldBe 2
      response1.value shouldBe Some(24.0)

      deviceActor ! Device.RecordTemperature(requestId = 3, 55.0, recordProbe.ref)
      recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 3))

      deviceActor ! Device.ReadTemperature(requestId = 4, readProbe.ref)
      val response2 = readProbe.receiveMessage()
      response2.requestId shouldBe 4
      response2.value shouldBe Some(55.0)
    }
  }
}
