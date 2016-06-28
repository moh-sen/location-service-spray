package locationservice

import scala.concurrent.duration.DurationInt

import com.typesafe.config.ConfigFactory

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.io.Tcp.{Bound, CommandFailed}
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http

object LocationServiceLauncher extends App {

  implicit val system = ActorSystem("location-service")

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val controller = system.actorOf(Props[LocationServiceController], "location-service-controller")

  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout = Timeout(10 seconds)
  implicit val executionContext = system.dispatcher

  IO(Http).ask(Http.Bind(controller, host, port)).mapTo[Http.Event].map {
    case Bound(address) => println(s"Successfully connected to $address")
    case CommandFailed(cmd) =>
      println(s"Could not connect to $host:$port, ${cmd.failureMessage}")
      system.shutdown()
  }
}