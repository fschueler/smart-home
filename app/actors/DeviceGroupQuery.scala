package actors

import actors.DeviceManager.{DeviceNotAvailable, DeviceTimedOut, RespondAllTemperatures}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}

import scala.concurrent.duration.FiniteDuration

/** Actor to query device groups.
  *
  * The actor queries all devices in a group and waits for their responses. It handles delays with timeouts,
  * device failures, and terminated devices.
  *
  * @param deviceIdToActor Map of all devices that are currently connected.
  * @param requestId Id of the current query.
  * @param requester The actor that sent the request.
  * @param timeout Timeout duration that the query uses to wait for device responses.
  * @param context Actor context.
  * @param timers The timer that is used to schedule timeout messages.
  */
class DeviceGroupQuery(
    deviceIdToActor: Map[String, ActorRef[Device.Command]],
    requestId: Long,
    requester: ActorRef[DeviceManager.RespondAllTemperatures],
    timeout: FiniteDuration,
    context: ActorContext[DeviceGroupQuery.Command],
    timers: TimerScheduler[DeviceGroupQuery.Command])
    extends AbstractBehavior[DeviceGroupQuery.Command](context) {

  import actors.DeviceGroupQuery._
  import actors.DeviceManager.{Temperature, TemperatureNotAvailable, TemperatureReading}

  timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)

  // convert the RespondTemperature replies from the device actor to the message protocol that this actor understands
  private val respondTemperatureAdapter = context.messageAdapter(WrappedRespondTemperature.apply)

  // query each device for temperature
  deviceIdToActor.foreach {
    case (deviceId, device) =>
      // watch each device so that we get notified if it terminates during our query
      context.watchWith(device, DeviceTerminated(deviceId))
      // ask each device for a temperature reading, converting the reply to our protocol
      device ! Device.ReadTemperature(0, respondTemperatureAdapter)
  }

  // keep track of replies and devices that have not yet replied
  private var repliesSoFar = Map.empty[String, TemperatureReading]
  private var stillWaiting = deviceIdToActor.keySet

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case WrappedRespondTemperature(response) => onRespondTemperature(response)
    case DeviceTerminated(deviceId) => onDeviceTerminated(deviceId)
    case CollectionTimeout => onCollectionTimeout()
  }

  private def onRespondTemperature(response: Device.RespondTemperature): Behavior[Command] = {
    val reading = response.value match {
      case Some(value) => Temperature(value)
      case None => TemperatureNotAvailable
    }

    val deviceId = response.deviceId
    repliesSoFar += deviceId -> reading
    stillWaiting -= deviceId

    respondIfAllCollected()
  }

  private def onDeviceTerminated(deviceId: String): Behavior[Command] = {
    if (stillWaiting(deviceId)) {
      repliesSoFar += deviceId -> DeviceNotAvailable
      stillWaiting -= deviceId
    }

    respondIfAllCollected()
  }

  private def onCollectionTimeout(): Behavior[Command] = {
    repliesSoFar ++= stillWaiting.map(deviceId => deviceId -> DeviceTimedOut)
    stillWaiting = Set.empty
    respondIfAllCollected()
  }

  private def respondIfAllCollected(): Behavior[Command] = {
    if (stillWaiting.isEmpty) {
      requester ! RespondAllTemperatures(requestId, repliesSoFar)
      Behaviors.stopped
    } else {
      this
    }
  }
}

object DeviceGroupQuery {

  def apply(
           deviceIdToActor: Map[String, ActorRef[Device.Command]],
           requestId: Long,
           requester: ActorRef[DeviceManager.RespondAllTemperatures],
           timeout: FiniteDuration
           ): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new DeviceGroupQuery(deviceIdToActor, requestId, requester, timeout, context, timers)
      }
    }
  }

  trait Command
  private case object CollectionTimeout extends Command
  final case class WrappedRespondTemperature(response: Device.RespondTemperature) extends Command
  private final case class DeviceTerminated(deviceId: String) extends Command

}
