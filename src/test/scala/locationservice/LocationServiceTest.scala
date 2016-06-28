package locationservice

import scala.concurrent.duration.DurationInt

import org.scalatest.{FunSuite, Matchers}

import akka.actor.ActorRefFactory
import spray.http.{ContentTypes, HttpEntity, HttpRequest}
import spray.http.StatusCodes.{MethodNotAllowed, OK}
import spray.json.pimpString
import spray.testkit.ScalatestRouteTest

class LocationServiceTest extends FunSuite with ScalatestRouteTest with LocationRestAPI with Matchers {
  def actorRefFactory: ActorRefFactory = system

  import Contents._
  
  import akka.util.Timeout
  import scala.concurrent.duration._
  implicit val routeTestTimeout = RouteTestTimeout(5.second)

  test("Location service should respond with correct location for a valid address") {
    makePostReques(Address) ~> locationServiceRoute ~> check {
      status shouldBe OK
      entity.data.asString shouldBe Location
    }
  }

  test("Location service should respond with correct error response for an invalid address") {
    makePostReques(InvalidAddress) ~> locationServiceRoute ~> check {
      status shouldBe OK
      entity.data.asString shouldBe ErrorEmptyResult
    }
  }
  
  test("Location service should respond with correct error response for an empty address") {
    makePostReques(EmptyAddress) ~> locationServiceRoute ~> check {
      rejections.head.toString should include("The given address cannot be empty")
      handled shouldBe false
    }
  }
  
  test("Location service only responds to Post requests") {
    tryUnsupportedMethod(Get())
    tryUnsupportedMethod(Put())
    tryUnsupportedMethod(Delete())
    
    def tryUnsupportedMethod(request: HttpRequest) = request ~> sealRoute(locationServiceRoute) ~> check {
      status shouldBe MethodNotAllowed
      responseAs[String] shouldBe "HTTP method not allowed, supported methods: POST"
    }
  }
  
  private def makePostReques(content: String) = Post("localhost/4242", HttpEntity(ContentTypes.`application/json`, content))
}

private object Contents {
  import spray.json._

  val Address = """{ "address" : "leidseplein 25, amsterdam" }""".parseJson.prettyPrint
  val Location = """{ "location": { "lat": 52.3635367, "lng": 4.882453 } }""".parseJson.prettyPrint
  
  val InvalidAddress = """{ "address" : "kjsd" }""".parseJson.prettyPrint
  val ErrorEmptyResult = """{ "error": { "message" : "No location could be bound to the given address!" } }""".parseJson.prettyPrint
  
  val EmptyAddress = """{ "address" : "" }""".parseJson.prettyPrint
}