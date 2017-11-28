package shop

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.StdIn


final case class ItemsEnvelope(items: List[Item])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import ItemJsonProtocol._

  implicit val itemsFormat: RootJsonFormat[Item] = jsonFormat4(Item)
  implicit val envelopeFormat: RootJsonFormat[ItemsEnvelope] = jsonFormat1(ItemsEnvelope)
}


object WebServer extends Directives with JsonSupport {
  import ProductCatalogManager._

  def main(args: Array[String]) {

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    implicit val timeout = Timeout(120 seconds)

    val catalog = system.actorOf(Props[ProductCatalogRouter])

    val route =
      path("products") {
        parameters('search) { (search) =>
          get {
            val futureItems = catalog ? SearchRequest(search)
            val response = Await.result(futureItems, timeout.duration).asInstanceOf[SearchResponse]
            complete(ItemsEnvelope(response.items))
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}