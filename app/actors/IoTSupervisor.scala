package actors

import actors.IoTSupervisor.Command
import akka.actor.typed._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

/** The toplevel actor (user guardian) for the IoT application.
  *
  * The IoTSupervisor manages all other device related actors.
  *
  * @param context Actor Context.
  */
class IoTSupervisor(context: ActorContext[Command])
    extends AbstractBehavior[Command](context) {
  context.log.info("IoT Supervisor started.")

  private var deviceManager: Option[ActorRef[DeviceManager.Command]] = None

  import IoTSupervisor._

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case Start(name, replyTo) =>
      deviceManager match {
        case Some(dm) =>
          replyTo ! StartupResponse(dm)
        case None =>
          val dm = context.spawn(DeviceManager(), name)
          context.watch(dm)
          deviceManager = Some(dm)
          replyTo ! StartupResponse(dm)
      }
      this

    case DeviceManagerTerminated(name) =>
      context.log.info(s"Device manager $name terminated.")
      this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("IoT Supervisor stopped.")
      this
  }
}

object IoTSupervisor {
  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      new IoTSupervisor(context)
    }
  }

  sealed trait Command
  final case class Start(name: String, replyTo: ActorRef[StartupResponse])
      extends Command
  final case class StartupResponse(
      deviceManager: ActorRef[DeviceManager.Command])

  private final case class DeviceManagerTerminated(name: String) extends Command
}
