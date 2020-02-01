package actors

import actors.DeviceGroup.{ReplyDeviceList, RequestDeviceList}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}

/** Actor that takes care of registering new devices.
  *
  * When a DeviceManager receives a request with a group and device id:
  *
  * - If the manager already has an actor for the device group, it forwards the request to it.
  * - Otherwise, it creates a new device group actor and then forwards the request.
  *
  */
class DeviceManager(context: ActorContext[DeviceManager.Command])
    extends AbstractBehavior[DeviceManager.Command](context) {
  import DeviceManager._

  var groupIdToActor = Map.empty[String, ActorRef[DeviceGroup.Command]]

  context.log.info("DeviceManager started.")

  override def onMessage(msg: Command): Behavior[Command] = msg match {
      // forward requests for new deviced to the responsible device group actor
    case trackMsg @ RequestTrackDevice(groupId, _, replyTo) =>
      groupIdToActor.get(groupId) match {
        case Some(ref) => ref ! trackMsg
        case None =>
          context.log.info("Creating device group actor for {}", groupId)
          val groupActor = context.spawn(DeviceGroup(groupId), s"group-$groupId")

          // watch the group actor so that we get notified when it terminates
          context.watchWith(groupActor, DeviceGroupTerminated(groupId))
          groupActor ! trackMsg
          groupIdToActor += groupId -> groupActor
      }
      this

    case req @ RequestDeviceList(requestId, groupId, replyTo) =>
      groupIdToActor.get(groupId) match {
        case Some(ref) => ref ! req
        case None =>
          replyTo ! ReplyDeviceList(requestId, Set.empty[String])
      }
      this

    case DeviceGroupTerminated(groupId) =>
      context.log.info("Device group actor for {} has been terminated.", groupId)
      groupIdToActor -= groupId
      this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceManager stopped.")
      this
  }
}

object DeviceManager {

  // device registration protocol
  trait Command
  final case class RequestTrackDevice(groupId: String,
                                      deviceId: String,
                                      replyTo: ActorRef[DeviceRegistered])
      extends DeviceManager.Command
      with DeviceGroup.Command

  final case class DeviceRegistered(device: ActorRef[Device.Command])
  private final case class DeviceGroupTerminated(groupId: String) extends Command

  // device query protocol
  final case class RequestAllTemperatures(
                                         requestId: Long,
                                         groupId: String,
                                         replyTo: ActorRef[RespondAllTemperatures]
                                         ) extends DeviceGroupQuery.Command
  with DeviceGroup.Command with DeviceManager.Command

  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  // reading states
  sealed trait TemperatureReading
  final case class Temperature(value: Double) extends TemperatureReading
  case object TemperatureNotAvailable extends TemperatureReading
  case object DeviceNotAvailable extends TemperatureReading
  case object DeviceTimedOut extends TemperatureReading
}
