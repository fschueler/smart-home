package model

import play.api.libs.json.{Json, Reads}


case class DHT22Reading(humidity: Double, temperature: Double) {

}

object DHT22Reading {
  implicit val reads: Reads[DHT22Reading] = Json.reads[DHT22Reading]
}
