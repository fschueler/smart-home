package actors

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike

class DeviceActorSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  import DeviceActor._

  "Device actor" must {
    "reply with empty reading if no temperature is known" in {
      val probe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(DeviceActor("group", "device"))

      deviceActor ! DeviceActor.ReadTemperature(requestId = 42, probe.ref)
      val response = probe.receiveMessage()
      response.requestId shouldBe 42
      response.value shouldBe None
    }
  }
}
