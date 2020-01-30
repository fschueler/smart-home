package actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

/** Device actor takes care of reading temperature values from a device.
  *
  * The device actor protocol includes two messages: One for requesting the last temperature reading and
  * one for returning the last read temperature value.
  *
  * @param context ActorContext.
  * @param groupId Id of the device group this actor belongs to.
  * @param deviceId Id of the device this actor reads from.
  */
class Device(context: ActorContext[Device.Command],
             groupId: String,
             deviceId: String)
    extends AbstractBehavior[Device.Command](context) {

  import Device._

  var lastReading: Option[Double] = None

  context.log.info("Device actor {}-{} started.", groupId, deviceId)

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case RecordTemperature(id, value, replyTo) =>
      context.log.info("Recorded temperature reading {} with {}", value, id)
      lastReading = Some(value)
      replyTo ! TemperatureRecorded(id)
      this

    case ReadTemperature(id, replyTo) =>
      replyTo ! RespondTemperature(id, lastReading)
      this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Device actor {}-{} stopped.", groupId, deviceId)
      this
  }
}

object Device {

  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))

  /** Device actor read protocol
    *
    * Device actors can be queried for temperature updates. To enable resend-instructions for failed requests
    * by other actors, we need to include request IDs into protocol messages. This allows us to correlate
    * requests and responses.
    */
  sealed trait Command
  final case class ReadTemperature(requestId: Long,
                                   replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  // write protocol
  final case class RecordTemperature(requestId: Long,
                                     value: Double,
                                     replyTo: ActorRef[TemperatureRecorded]) extends Command
  final case class TemperatureRecorded(requestId: Long)
}
