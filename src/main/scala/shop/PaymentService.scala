package shop

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Terminated}
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object PaymentService {

  case class DoPayment(paymentType: String)
  case object PaymentConfirmed
  case object PaymentReceived

}

class PaymentService(customer: ActorRef, checkout: ActorRef) extends Actor with ActorLogging {

  val MAX_RETRIES = 5

  var current_retry = 1

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(loggingEnabled = false) {
    case _: BadRequestException =>
      log.warning("Bad request")

      if(current_retry <= MAX_RETRIES) {
        current_retry += 1
        Restart
      } else {
        Stop
      }
    case _: InvalidPaymentException =>
      log.warning("Payment Invalid")

      if(current_retry <= MAX_RETRIES) {
        current_retry += 1
        Restart
      } else {
        Stop
      }
    case e =>
      log.error("Unexpected failure: {}", e.getMessage)
      Stop
  }


  def receive = LoggingReceive {
    case PaymentService.DoPayment(paymentType: String) =>
      context.actorOf(Props(new PaymentHTTPWorker(paymentType, customer, checkout)))
  }
}



object PaymentHTTPWorker {
  case object PaymentConfirmed
}

class PaymentHTTPWorker(paymentType: String, customer: ActorRef, checkout: ActorRef) extends Actor
  with ActorLogging {

  import PaymentService._

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(context.system)

  override def preStart(): Unit = {
    http.singleRequest(HttpRequest(uri = "http://httpbin.org/status/500"))
      .pipeTo(self)
  }

  def receive: Receive = LoggingReceive {
    case resp @ HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
            println("Got response, body: " + body.utf8String)
        resp.discardEntityBytes()

        println("Success! Confirming payment!")
        customer ! PaymentConfirmed
        checkout ! Checkout.PaymentReceived

        shutdown()
      }

    case resp @ HttpResponse(StatusCodes.BadRequest, _, _, _) =>
      println("Bad request")
      resp.discardEntityBytes()
      throw new BadRequestException
      shutdown()

    case resp @ HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
      throw new InvalidPaymentException
      shutdown()
  }

 def shutdown(): Future[Terminated] = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
 }
}

class BadRequestException extends Exception("Bad request")
class InvalidPaymentException extends Exception("Repeat")
