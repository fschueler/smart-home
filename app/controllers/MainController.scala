package controllers

import akka.util.Timeout
import play.api.mvc._
import services.DHT22Service

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class MainController(components: ControllerComponents, dHT22Service: DHT22Service)(implicit ec: ExecutionContext) extends AbstractController(components) {

  implicit private val timeout: Timeout = 5 seconds

  def index = Action.async { implicit request =>
    val dht22ReadingF = dHT22Service.getReading

    dht22ReadingF.map {
      case Right(reading) => Ok(views.html.index(reading))
      case Left(error) => BadRequest(error)
    }
  }
}
