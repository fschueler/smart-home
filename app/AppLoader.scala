import play.api.ApplicationLoader.Context
import play.api._
import play.api.mvc._
import com.softwaremill.macwire._
import _root_.controllers.AssetsComponents
import _root_.controllers.MainController
import play.api.libs.ws.ahc.AhcWSComponents
import play.filters.HttpFiltersComponents
import router.Routes
import services.DHT22Service

class AppApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      configurator =>
        configurator.configure(context.environment)
    }
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
  override lazy val router = wire[Routes]

  // controllers
  lazy val mainController = wire[MainController]

  // services
  lazy val dht22Service = wire[DHT22Service]
}
