package controllers

import actors.DeviceManager
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import play.api.mvc._
import services.DHT22Service
import akka.actor.typed.scaladsl.AskPattern._
import play.api.{Logger, Logging}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class MainController(components: ControllerComponents,
                     dHT22Service: DHT22Service,
                     deviceManager: ActorRef[DeviceManager.Command])(
    implicit ec: ExecutionContext,
    scheduler: Scheduler)
    extends AbstractController(components) {
  private val logger: Logger = Logger(this.getClass())
  implicit private val timeout: Timeout = 5 seconds

  def index = Action.async { implicit request =>
    logger.info("Serving index.")

    val dht22ReadingF = dHT22Service.getReading

    dht22ReadingF.map {
      case Right(reading) => Ok(views.html.index(reading))
      case Left(error)    => BadRequest(error)
    }
  }

  def register(groupId: String, deviceId: String) = Action.async { request =>
    val responseF: Future[DeviceManager.DeviceRegistered] = deviceManager.ask(
      replyTo => DeviceManager.RequestTrackDevice(groupId, deviceId, replyTo))

    responseF map { deviceId =>
      logger.info(s"Registered new device $deviceId")
      Ok
    } recover {
      case e: Exception =>
        logger.error(s"Could not register device $deviceId")
        InternalServerError(e.getMessage)
    }
  }
}
