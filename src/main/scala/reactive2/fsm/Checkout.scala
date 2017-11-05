package reactive2.fsm

import akka.actor.{ActorRef, FSM}
import shop.Item

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._



case class DeliveryMethodSelected(method: String)
case object Cancelled
case object CheckoutTimerExpired
case object PaymentTimerExpired
case class PaymentSelected(method: String)
case object PaymentReceived


sealed trait CheckoutState
case object ProcessingPayment extends CheckoutState
case object SelectingPaymentMethod extends CheckoutState
case object SelectingDelivery extends CheckoutState


sealed trait CheckoutData
case object Uninitialized extends CheckoutData
case class CheckoutInfo(paymentMethod: String, deliveryMethod: String) extends CheckoutData


class Checkout(cart: ActorRef) extends FSM[CheckoutState, CheckoutData] {

  startWith(SelectingDelivery, Uninitialized)

  when(SelectingDelivery) {
    case Event(DeliveryMethodSelected(method), Uninitialized) =>
      print("Selecting Delivery" + method + " \n")
      goto(SelectingPaymentMethod) using CheckoutInfo("", method)
  }

  when(SelectingPaymentMethod) {
    case Event(PaymentSelected(method), info @ CheckoutInfo(_, delivery)) =>
      print("Selecting Payment Method" + method + " \n")
      goto(ProcessingPayment) using CheckoutInfo(method, delivery)
  }

  when(ProcessingPayment) {
    case Event(PaymentReceived, info: CheckoutInfo) =>
      print("Payment Received\n")
      cart ! CheckoutClosed
      stop()

    case Event(PaymentTimerExpired, _) =>
      cart ! CheckoutCancelled
      stop()

  }

  whenUnhandled {
    case Event(Cancelled, _) =>
      print("Checkout cancelled")
      cart ! CheckoutCancelled
      stop()

    case Event(CheckoutTimerExpired, _) =>
      cart ! CheckoutCancelled
      stop()
  }

  onTransition {
    case _ -> SelectingDelivery => setTimer("checkoutTimer", CheckoutTimerExpired, 3 seconds)
    case SelectingPaymentMethod -> ProcessingPayment =>
      cancelTimer("checkoutTimer")
      setTimer("paymentTimer", PaymentTimerExpired, 3 seconds)
  }

  initialize()
}
