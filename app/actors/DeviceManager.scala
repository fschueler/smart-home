package actors

import akka.actor.typed.ActorRef

/** Actor that takes care of registering new devices.
  *
  * When a DeviceManager receives a request with a group and device id:
  *
  * - If the manager already has an actor for the device group, it forwards the request to it.
  * - Otherwise, it creates a new device group actor and then forwards the request.
  *
  */
class DeviceManager {

}

object DeviceManager {

  // device registration protocol
  sealed trait Command
  final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered]) extends DeviceManager.Command with DeviceGroup.Command

  final case class DeviceRegistered(device: ActorRef[Device.Command])
}
