package actors

import actors.DeviceGroupQuery.WrappedRespondTemperature
import actors.DeviceManager.{RespondAllTemperatures, Temperature}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike

import scala.concurrent.duration._

class GroupQuerySpec extends ScalaTestWithActorTestKit with WordSpecLike {

  "GroupQuery actor" must {
    "return temperature value for working devices" in {
      val requester = createTestProbe[RespondAllTemperatures]()

      val device1 = createTestProbe[Device.Command]()
      val device2 = createTestProbe[Device.Command]()

      val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

      // initialize query actor to query existing devices 1 and 2
      val queryActor = spawn(DeviceGroupQuery(deviceIdToActor,
                         requestId = 1,
                         requester = requester.ref,
                         timeout = 3.seconds))

      // the devices should receive a ReadTemperature message from the query actor
      device1.expectMessageType[Device.ReadTemperature]
      device2.expectMessageType[Device.ReadTemperature]

      // we manually respond to the query actor
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))


      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))
        )
      )
    }
  }
}
