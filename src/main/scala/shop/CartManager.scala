package shop

import akka.actor.{ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.duration._

object CartManager {
  sealed trait CartState
  case object Empty extends CartState
  case class NonEmpty(timestamp: Long) extends CartState
  case object InCheckout extends CartState

  case object StartCheckout
  case class CheckoutStarted(checkout: ActorRef)
  case object CheckoutCancelled
  case object CheckoutClosed
  case object CartTimeExpired
  case object CartEmpty

  sealed trait Event

  case class AddItem(item: Item, count: Int) extends Event
  case class ItemAdded(item: Item, count: Int)
  case class RemoveItem(item: Item, count: Int) extends Event
  case class ItemRemoved(item: Item, count: Int)
  case object ClearCart extends Event

  private case class ChangeState(state: CartState) extends Event
}


class CartManager(customer: ActorRef, var shoppingCart: Cart) extends PersistentActor with Timers {
  import CartManager._

  val cartTimeout: FiniteDuration = 120 seconds

  override def persistenceId = "persistent-cart-manager-id-1"

  def this(customer: ActorRef) = {
    this(customer, Cart.empty)
  }

  private def startTimer(timestamp: Long, time: FiniteDuration): Unit = {
    timers.startSingleTimer("cart-timer-" + timestamp, CartTimeExpired, time)
  }

  private def cancelTimer(): Unit = {
    timers.cancelAll()
  }

  private def updateState(event: Event): Unit = {
    event match {
      case ClearCart => shoppingCart = Cart.empty
      case AddItem(item, count) => shoppingCart = shoppingCart.addItem(item, count)
      case RemoveItem(item, count) => shoppingCart = shoppingCart.removeItem(item.id, count)
      case ChangeState(state) =>
        state match {
            case Empty =>
              cancelTimer()
              context become empty
            case NonEmpty(timestamp) =>
              val now = System.currentTimeMillis()
              val diff = Math.max((now - timestamp) / 1000.0, 0)

              print("Mam " + shoppingCart.itemsCount() + " itemy \n")
              print("Zostalo " + (cartTimeout - diff.seconds) + " sekund \n")

              startTimer(timestamp, cartTimeout - diff.seconds)
              context become nonEmpty
            case InCheckout =>
              cancelTimer()
              context become inCheckout
        }
    }
  }

  private def becomeEmpty(): Unit = {
    persist(ClearCart) { event =>
      updateState(event)
      customer ! CartEmpty
      persist(ChangeState(Empty)) { event => updateState(event)}
    }
  }

  private def becomeNonEmpty(): Unit = {
    val now = System.currentTimeMillis()
    persist(ChangeState(NonEmpty(now))) { event => updateState(event) }
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item, count) =>
      persist(AddItem(item, count)) { event =>
          updateState(event)
          sender ! ItemAdded(item, count)
          becomeNonEmpty()
      }
  }

  def nonEmpty: Receive = LoggingReceive {

    case CartTimeExpired => becomeEmpty()
    case ClearCart => becomeEmpty()

    case AddItem(item, count) =>
      persist(AddItem(item, count)) { event =>
        updateState(event)
        print("Mam " + shoppingCart.itemsCount() + " itemy \n")
        sender ! ItemAdded(item, count)
      }

    case RemoveItem(item, count) =>
      persist(RemoveItem(item, count)) { event =>
          updateState(event)
          sender ! ItemRemoved(item, count)

          if (shoppingCart.itemsCount() == 0) becomeEmpty()
      }

    case StartCheckout =>
      customer ! CheckoutStarted(createCheckout())
      persist(ChangeState(InCheckout)) { event => updateState(event) }
  }

  def createCheckout(): ActorRef = {
    context.actorOf(Props(new Checkout(customer, self)), "checkout")
  }

  def inCheckout: Receive = LoggingReceive {
    case CheckoutClosed => becomeEmpty()
    case CheckoutCancelled => becomeNonEmpty()
  }

  override def receiveCommand: Receive = empty

  override def receiveRecover: Receive = LoggingReceive {
    case event: Event => updateState(event)
    case SnapshotOffer(_, snapshot: Cart) => shoppingCart = snapshot
  }
}
