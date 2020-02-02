import play.api.ApplicationLoader.Context
import play.api._
import play.api.mvc._
import com.softwaremill.macwire._
import _root_.controllers.AssetsComponents
import _root_.controllers.MainController
import actors.{DeviceManager, IoTSupervisor}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import play.api.libs.ws.ahc.AhcWSComponents
import play.filters.HttpFiltersComponents
import router.Routes
import services.DHT22Service

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class AppApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
        _.configure(context.environment, context.initialConfiguration, Map.empty)    }
    new AppComponents(context).application
  }
}

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AssetsComponents
    with AhcWSComponents {

  val log = Logger(this.getClass)

  override lazy val controllerComponents = wire[DefaultControllerComponents]
  lazy val prefix = "/"
  override lazy val router = wire[Routes]

  // actor system
  lazy val typedActorSystem =
    ActorSystem[IoTSupervisor.Command](IoTSupervisor(), "iot-system")

  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val actorScheduler: akka.actor.typed.Scheduler = typedActorSystem.scheduler

  val deviceManagerF: Future[IoTSupervisor.StartupResponse] =
    typedActorSystem.ask(replyTo =>
      IoTSupervisor.Start("device-manager", replyTo))

  lazy val deviceManager: ActorRef[DeviceManager.Command] =
    Await.result(deviceManagerF, 3.seconds).deviceManager

  // controllers
  lazy val mainController = wire[MainController]

  // services
  lazy val dht22Service = wire[DHT22Service]
}
