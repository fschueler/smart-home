package services

import config.DHT22ServiceConfig
import model.DHT22Reading
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.http.Status.OK

import scala.concurrent.{ExecutionContext, Future}

class DHT22Service(configuration: Configuration, wSClient: WSClient)(implicit ec: ExecutionContext) {

  private val config: DHT22ServiceConfig = configuration.get[DHT22ServiceConfig]("dht22service")
  private val dht22Url: String = s"http://${config.host}:${config.port}"


  def getReading: Future[Either[String, DHT22Reading]] = {
    val readingResponseF = wSClient.url(dht22Url + "/api/read").get()

    readingResponseF.map { response =>
      response.status match {
        case OK => Right(response.json.as[DHT22Reading])
        case _ => Left(response.body)
      }
    }.recover {
      case th => Left(th.getMessage)
    }
  }

}
