package shop

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._


object Checkout {
  private case object TimerKey
  case class DeliveryMethodSelected(method: String)

  case object Cancelled
  case object CheckoutTimerExpired
  case object PaymentTimerExpired
  case class PaymentSelected(method: String)
  case object PaymentReceived

  case class PaymentServiceStarted(payment: ActorRef)
}

class Checkout(customer: ActorRef, cart: ActorRef) extends Actor with Timers {
  import Checkout._

  var paymentMethod = "default"
  var deliveryMethod = "default"


  private def startCheckoutTimer(): Unit = {
    timers.startSingleTimer(TimerKey, CheckoutTimerExpired, 3 seconds)
  }

  private def startPaymentTimer(): Unit = {
    timers.startSingleTimer(TimerKey, PaymentTimerExpired, 3 seconds)
  }

  override def preStart(): Unit = {
    super.preStart()
    startCheckoutTimer()
  }

  def selectingDelivery: Receive = LoggingReceive {
    case DeliveryMethodSelected(method) =>
      deliveryMethod = method
      context become selectingPaymentMethod
    case (Cancelled | CheckoutTimerExpired) =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)
  }

  def selectingPaymentMethod: Receive = LoggingReceive {
    case PaymentSelected(method) =>
      paymentMethod = method

      val paymentService = createPayment()
      customer ! PaymentServiceStarted(paymentService)

      startPaymentTimer()
      context become processingPayment
    case (Cancelled | CheckoutTimerExpired) =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)
  }

  def createPayment(): ActorRef = {
    context.actorOf(Props(new PaymentService(customer, self)),  "paymentsService")
  }

  def processingPayment: Receive = LoggingReceive {
    case PaymentReceived =>
      customer ! Cart.CheckoutClosed
      cart ! Cart.CheckoutClosed
      context.stop(self)
    case (Cancelled | PaymentTimerExpired) =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)
  }


  def receive: Receive = selectingDelivery
}
