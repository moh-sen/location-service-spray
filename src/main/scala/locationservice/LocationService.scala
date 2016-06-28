package locationservice

import java.net.URLEncoder

import scala.util.{Failure, Success}

import LocationServiceProtocol.{sprayJsonMarshaller, sprayJsonUnmarshaller}
import akka.actor.{Actor, ActorLogging}
import spray.client.pipelining.{Get, WithTransformerConcatenation, sendReceive, sendReceive$default$3, unmarshal}
import spray.httpx.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsonFormat}
import spray.routing.RequestContext

case class Address(address: String) {
  require(!address.isEmpty(), "The given address cannot be empty.")
}

case class AddressComponent(long_name: String, short_name: String, types: List[String])
case class Location(lat: Double, lng: Double)
case class Viewport(northeast: Location, southwest: Location)
case class Geometry(location: Location, location_type: String, viewport: Viewport)
case class SingleResult(address_components: List[AddressComponent],
  formatted_address: String, geometry: Geometry, place_id: String, types: List[String])
case class GoogleMapsResponse[T](results: List[T], status: String)

case class Error(message: String)
case class LocationResponse(location: Option[Location], error: Option[Error])

private object LocationServiceProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  val GoogleMapsAddressService = "https://maps.googleapis.com/maps/api/geocode/json?address="

  implicit val addressFormat = jsonFormat1(Address)

  implicit val addressComponentFormat = jsonFormat3(AddressComponent)
  implicit val locationFormat = jsonFormat2(Location)
  implicit val viewportFormat = jsonFormat2(Viewport)
  implicit val geometryFormat = jsonFormat3(Geometry)
  implicit val singleResultFormat = jsonFormat5(SingleResult)
  implicit def googleMapsResponse[T: JsonFormat] = jsonFormat2(GoogleMapsResponse.apply[T])
  
  implicit val errorFormat = jsonFormat1(Error)
  implicit val locationResponseFormat = jsonFormat2(LocationResponse)
}

private object GoogleResponseStatusCodes {
  val Zero = "ZERO_RESULTS"
  val Ok = "OK"
}

class LocationService(requestContext: RequestContext) extends Actor with ActorLogging {

  import system.dispatcher
  import LocationServiceProtocol._
  import GoogleResponseStatusCodes._

  implicit val system = context.system

  def receive = {
    case Address(address) =>
      log.info(s"Received location request for the address: $address")
      getLocation(address)
      context.stop(self)
    case other            =>
      log.debug(s"Received an unknown message: $other")
      context.stop(self)
  }
  
  def getLocation(address: String) = {
    val pipeline = sendReceive ~> unmarshal[GoogleMapsResponse[SingleResult]]

    pipeline {
      Get(s"$GoogleMapsAddressService${URLEncoder.encode(address, "UTF-8")}")
    } onComplete {
      case Success(googleMapsResult) =>
        val response = (googleMapsResult.status, googleMapsResult.results.size) match {
          case (Ok, single)                  =>
            val location = googleMapsResult.results.head.geometry.location
            LocationResponse(Some(location), None)
          case (Zero, 0)                     =>
            LocationResponse(None, Some(Error("No location could be bound to the given address!")))
          case (_, multiple) if multiple > 1 =>
            LocationResponse(None, Some(Error("Multiple locations can be bound to the given address!")))
          case (invalidStatus, _)            =>
            LocationResponse(None, Some(Error("Received an invalid status from Google: $invalidStatus")))
        }
        requestContext.complete(response)
      case Failure(error)            =>
        requestContext.complete(error)
    }
  }

}