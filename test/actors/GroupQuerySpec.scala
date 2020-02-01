package actors

import actors.DeviceGroupQuery.WrappedRespondTemperature
import actors.DeviceManager._
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike

import scala.concurrent.duration._

class GroupQuerySpec extends ScalaTestWithActorTestKit with WordSpecLike {

  "GroupQuery actor" must {
    "return temperature value for working devices" in {
      val requester = createTestProbe[RespondAllTemperatures]()

      val device1 = createTestProbe[Device.Command]()
      val device2 = createTestProbe[Device.Command]()

      val deviceIdToActor =
        Map("device1" -> device1.ref, "device2" -> device2.ref)

      // initialize query actor to query existing devices 1 and 2
      val queryActor = spawn(
        DeviceGroupQuery(deviceIdToActor,
                         requestId = 1,
                         requester = requester.ref,
                         timeout = 5.seconds))

      // the devices should receive a ReadTemperature message from the query actor
      device1.expectMessageType[Device.ReadTemperature]
      device2.expectMessageType[Device.ReadTemperature]

      // we manually respond to the query actor
      queryActor ! WrappedRespondTemperature(
        Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))
      queryActor ! WrappedRespondTemperature(
        Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures =
            Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))
        )
      )
    }

    "return TemperatureNotAvailable for devices with no readings" in {
      val requester = createTestProbe[RespondAllTemperatures]()

      val device1 = createTestProbe[Device.Command]()
      val device2 = createTestProbe[Device.Command]()

      val deviceIdToActor =
        Map("device1" -> device1.ref, "device2" -> device2.ref)

      val queryActor =
        spawn(
          DeviceGroupQuery(deviceIdToActor,
                           requestId = 1,
                           requester = requester.ref,
                           timeout = 5.seconds))

      device1.expectMessageType[Device.ReadTemperature]
      device2.expectMessageType[Device.ReadTemperature]

      queryActor ! WrappedRespondTemperature(
        Device.RespondTemperature(requestId = 0, "device1", None))
      queryActor ! WrappedRespondTemperature(
        Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map("device1" -> TemperatureNotAvailable,
                             "device2" -> Temperature(2.0))
        )
      )
    }

    "return DeviceNotAvailable if device stops before answering" in {
      val requester = createTestProbe[RespondAllTemperatures]()

      val device1 = createTestProbe[Device.Command]()
      val device2 = createTestProbe[Device.Command]()

      val deviceIdToActor =
        Map("device1" -> device1.ref, "device2" -> device2.ref)

      val queryActor =
        spawn(
          DeviceGroupQuery(deviceIdToActor,
                           requestId = 1,
                           requester = requester.ref,
                           timeout = 5.seconds))

      device1.expectMessageType[Device.ReadTemperature]
      device2.expectMessageType[Device.ReadTemperature]

      // device 1 responds with temp
      queryActor ! WrappedRespondTemperature(
        Device.RespondTemperature(requestId = 0, "device1", Some(2.0)))

      // device 2 stops during query
      device2.stop()

      requester.expectMessage(
        RespondAllTemperatures(requestId = 1,
                               temperatures =
                                 Map("device1" -> Temperature(2.0),
                                     "device2" -> DeviceNotAvailable)))
    }

    "return temperature reading even if device stops after answering" in {
      val requester = createTestProbe[RespondAllTemperatures]()

      val device1 = createTestProbe[Device.Command]()
      val device2 = createTestProbe[Device.Command]()

      val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

      val queryActor =
        spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

      device1.expectMessageType[Device.ReadTemperature]
      device2.expectMessageType[Device.ReadTemperature]

      // both devices respond with temps
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))
      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

      // device 2 stops after it already sent its reading
      device2.stop()

      // we still expect the reading, even though the query actor receives a Terminated message
      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))))
    }

    "return DeviceTimedOut if device does not answer in time" in {
      val requester = createTestProbe[RespondAllTemperatures]()

      val device1 = createTestProbe[Device.Command]()
      val device2 = createTestProbe[Device.Command]()

      val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

      val queryActor =
        spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 200.millis))

      device1.expectMessageType[Device.ReadTemperature]
      device2.expectMessageType[Device.ReadTemperature]

      queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))

      // no reply from device2

      requester.expectMessage(
        RespondAllTemperatures(
          requestId = 1,
          temperatures = Map("device1" -> Temperature(1.0), "device2" -> DeviceTimedOut)))
    }
  }
}
