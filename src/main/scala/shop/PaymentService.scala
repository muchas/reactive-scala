package shop

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object PaymentService {
  case object PaymentConfirmed
}

class PaymentService(customer: ActorRef, checkout: ActorRef) extends Actor
  with ActorLogging {

  import PaymentService._

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(context.system)

  override def preStart(): Unit = {
    http.singleRequest(HttpRequest(uri = "http://localhost:8080/hello"))
      .pipeTo(self)
  }

//  def receive: Receive = LoggingReceive {
//    case Customer.DoPayment =>
//      customer ! PaymentConfirmed
//      checkout ! Checkout.PaymentReceived
//      context.stop(self)
//  }

  def receive: Receive = LoggingReceive {
    case resp @ HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
            println("Got response, body: " + body.utf8String)
        resp.discardEntityBytes()

        customer ! PaymentConfirmed
        checkout ! Checkout.PaymentReceived

        shutdown()
      }

    case resp @ HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
      shutdown()
  }

 def shutdown(): Future[Terminated] = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
 }
}
