package shop

import akka.actor.{ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.PersistentActor

import scala.concurrent.duration._


object Checkout {

  // Protocol
  case object DeliveryMethodRequest
  case class DeliveryMethodResponse(method: String)

  case object PaymentMethodRequest
  case class PaymentMethodResponse(method: String)

  case object StateRequest
  case class StateResponse(state: CheckoutState)

  case object Cancelled
  case object CheckoutTimerExpired
  case object PaymentTimerExpired
  case object PaymentReceived
  case class PaymentServiceStarted(payment: ActorRef)

  // States
  sealed trait CheckoutState
  case class SelectingDelivery(timestamp: Long) extends CheckoutState
  case class SelectingPaymentMethod(timestamp: Long) extends CheckoutState
  case class ProcessingPayment(timestamp: Long) extends CheckoutState
  case object CheckoutCancelled extends CheckoutState
  case object CheckoutClosed extends CheckoutState

  // Events
  sealed trait Event
  case class PaymentSelected(method: String) extends Event
  case class DeliveryMethodSelected(method: String) extends Event
  private case class StateChanged(state: CheckoutState) extends Event
}

class Checkout(customer: ActorRef, cart: ActorRef, id: String) extends PersistentActor with Timers {
  import Checkout._

  val checkoutTimeout: FiniteDuration = 120 seconds
  val paymentTimeout: FiniteDuration = 120 seconds

  var paymentMethod = "default"
  var deliveryMethod = "default"

  override def persistenceId: String = id

  def this(customer: ActorRef, cart: ActorRef) = {
    this(customer, cart, "persistent-checkout-id-1")
  }

  private def startCheckoutTimer(timestamp: Long, time: FiniteDuration): Unit = {
    timers.startSingleTimer("checkout-timer-" + timestamp, CheckoutTimerExpired, time)
  }

  private def startPaymentTimer(timestamp: Long, time: FiniteDuration): Unit = {
    timers.startSingleTimer("payment-timer-" + timestamp, PaymentTimerExpired, time)
  }

  private def cancelTimers(): Unit = {
    timers.cancelAll()
  }

  private def calculateElapsedTime(timestamp: Long): FiniteDuration = {
    val now = System.currentTimeMillis()
    val diff = Math.max((now - timestamp) / 1000.0, 0)
    diff.seconds
  }

  private def updateState(event: Event): Unit = {
    event match {
      case DeliveryMethodSelected(method) => deliveryMethod = method
      case PaymentSelected(method) => paymentMethod = method
      case StateChanged(state) =>
        state match {
          case SelectingDelivery(timestamp) =>
            cancelTimers()
            startCheckoutTimer(timestamp, checkoutTimeout - calculateElapsedTime(timestamp))
            context become selectingDelivery

          case SelectingPaymentMethod(timestamp) =>
            cancelTimers()
            startCheckoutTimer(timestamp, checkoutTimeout - calculateElapsedTime(timestamp))
            context become selectingPaymentMethod

          case ProcessingPayment(timestamp) =>
            cancelTimers()
            startPaymentTimer(timestamp, paymentTimeout - calculateElapsedTime(timestamp))
            context become processingPayment

          case CheckoutCancelled =>
            context.stop(self)
        }
    }
  }

  override def preStart(): Unit = {
    super.preStart()
    startCheckoutTimer(System.currentTimeMillis(), checkoutTimeout)
  }

  def selectingDelivery: Receive = LoggingReceive {
    case DeliveryMethodSelected(method) =>
      val now = System.currentTimeMillis()
      persist(DeliveryMethodSelected(method)) { event =>
        updateState(event)
        persist(StateChanged(SelectingPaymentMethod(now))) { event =>
          updateState(event)
          customer ! DeliveryMethodSelected(method)
        }
      }

    case (Cancelled | CheckoutTimerExpired) =>
      persist(StateChanged(CheckoutCancelled)) { event =>
        cart ! CartManager.CheckoutCancelled
        updateState(event)
      }

    case StateRequest => sender ! StateResponse(SelectingDelivery(0))
  }

  def selectingPaymentMethod: Receive = LoggingReceive {
    case PaymentSelected(method) =>
      val now = System.currentTimeMillis()
      persist(PaymentSelected(method)) { event =>
        updateState(event)

        persist(StateChanged(ProcessingPayment(now))) { event =>
          updateState(event)
          customer ! PaymentServiceStarted(createPayment())
        }
      }

    case (Cancelled | CheckoutTimerExpired) =>
      persist(StateChanged(CheckoutCancelled)) { event =>
        cart ! CartManager.CheckoutCancelled
        updateState(event)
      }

    case DeliveryMethodRequest => sender ! DeliveryMethodResponse(deliveryMethod)
    case StateRequest => sender ! StateResponse(SelectingPaymentMethod(0))
  }

  def createPayment(): ActorRef = {
    context.actorOf(Props(new PaymentService(customer, self)),  "paymentsService")
  }

  def processingPayment: Receive = LoggingReceive {
    case PaymentReceived =>
      persist(StateChanged(CheckoutClosed)) { event =>
        customer ! CartManager.CheckoutClosed
        cart ! CartManager.CheckoutClosed
        updateState(event)
      }

    case (Cancelled | PaymentTimerExpired) =>
      persist(StateChanged(CheckoutCancelled)) { event =>
        cart ! CartManager.CheckoutCancelled
        updateState(event)
      }

    case DeliveryMethodRequest => sender ! DeliveryMethodResponse(deliveryMethod)
    case PaymentMethodRequest => sender ! PaymentMethodResponse(paymentMethod)
    case StateRequest => sender ! StateResponse(ProcessingPayment(0))
  }

  override def receiveCommand: Receive = selectingDelivery
  override def receiveRecover: Receive = LoggingReceive {
    case event: Event => updateState(event)
  }
}
