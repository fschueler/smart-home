package actors

import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

/** The toplevel actor (user guardian) for the IoT application.
  *
  * The IoTSupervisor manages all other device related actors.
  *
  * @param context Actor Context.
  */
class IoTSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info("IoT Supervisor started.")

  // Not handling any messages in this actor
  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info("IoT Supervisor stopped.")
      this
  }
}

object IoTSupervisor {
  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Nothing](context => new IoTSupervisor(context))
  }
}
