package controllers

import akka.util.Timeout
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import scala.language.postfixOps

class MainController(components: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(components) {

  implicit private val timeout: Timeout = 5 seconds

  def index = Action { implicit request =>
    Ok(views.html.index())
  }
}
