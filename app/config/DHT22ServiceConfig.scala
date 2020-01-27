package config

import com.typesafe.config.Config
import play.api.ConfigLoader

case class DHT22ServiceConfig(host: String, port: Int, pollInterval: Int)

object DHT22ServiceConfig {
  implicit val configLoader: ConfigLoader[DHT22ServiceConfig] =
    (rootConfig: Config, path: String) => {
      val config = rootConfig.getConfig(path)
      DHT22ServiceConfig(
        host = config.getString("host"),
        port = config.getInt("port"),
        pollInterval = config.getInt("pollInterval")
      )
    }
}
