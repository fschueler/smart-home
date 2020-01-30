package actors

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

/** Represents a group of devices in some logical grouping (e.g. different rooms, houses, etc.).
  *
  * A device group handles device registration by looking up devices and returning their reference or
  * creating new actors for them.
  * The device group actor also keeps track of existing devices and stops actors when devices are removed.
  *
  * @param context Actor Context.
  * @param groupId ID of the group that this actor manages.
  */
class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{DeviceRegistered, RequestTrackDevice}

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    // match track device requests for this actor's group ID
    case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
      deviceIdToActor.get(deviceId) match {
        case Some(deviceActor) =>
          replyTo ! DeviceRegistered(deviceActor)
        case None =>
          context.log.info("Creating device actor for {}", trackMsg.deviceId)
          val deviceActor =
            context.spawn(Device(groupId, deviceId), s"device-$deviceId")

          // watch the newly created actor so that we get notified when it terminates
          context.watchWith(deviceActor,
                            DeviceTerminated(deviceActor, groupId, deviceId))

          deviceIdToActor += deviceId -> deviceActor
          replyTo ! DeviceRegistered(deviceActor)
      }
      this

    // ignore track device requests for other group IDs.
    case RequestTrackDevice(gId, _, _) =>
      context.log.warn(
        "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
        gId,
        groupId)
      this

    case RequestDeviceList(requestId, gId, replyTo) =>
      if (gId == groupId) {
        replyTo ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
        this
      } else {
        Behaviors.unhandled
      }

    // this is sent when a watched actor stops.
    case DeviceTerminated(_, _, deviceId) =>
      context.log.info("Device actor for {} has been terminated", deviceId)
      deviceIdToActor -= deviceId
      this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped.", groupId)
      this
  }
}

object DeviceGroup {
  def apply(groupId: String): Behavior[Command] =
    Behaviors.setup(context => new DeviceGroup(context, groupId))

  // command is extended by device manager
  trait Command
  private final case class DeviceTerminated(device: ActorRef[Device.Command],
                                            groupId: String,
                                            deviceId: String)
      extends Command

  final case class RequestDeviceList(requestId: Long,
                                     groupId: String,
                                     replyTo: ActorRef[ReplyDeviceList])
      extends DeviceManager.Command
      with DeviceGroup.Command
  final case class ReplyDeviceList(requestId: Long, ids: Set[String])
}
