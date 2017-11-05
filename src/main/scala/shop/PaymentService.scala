package shop

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive


object PaymentService {
  case object PaymentConfirmed
}

class PaymentService(customer: ActorRef, checkout: ActorRef) extends Actor {
  import PaymentService._

  def receive: Receive = LoggingReceive {
    case Customer.DoPayment =>
      customer ! PaymentConfirmed
      checkout ! Checkout.PaymentReceived
      context.stop(self)
  }
}
