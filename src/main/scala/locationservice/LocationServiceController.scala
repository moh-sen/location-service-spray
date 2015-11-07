package locationservice

import akka.actor.{ Actor, Props }
import spray.routing.Directive.pimpApply
import spray.routing.HttpService

trait LocationRestAPI extends HttpService {
  import locationservice.LocationServiceProtocol._

  val locationServiceRoute =
    post {
      entity(as[Address]) { address => requestContext =>
          val locationService = actorRefFactory.actorOf(Props(new LocationService(requestContext)), "location-service")
          locationService ! address
      }
    }
}

class LocationServiceController extends Actor with LocationRestAPI {
  def actorRefFactory = context
  
  def receive = runRoute(locationServiceRoute)
}