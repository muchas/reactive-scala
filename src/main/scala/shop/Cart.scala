package shop

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.LoggingReceive

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._


object Cart {
  private case object TimerKey

  case object StartCheckout
  case class CheckoutStarted(checkout: ActorRef)
  case object CheckoutCancelled
  case object CheckoutClosed
  case object CartTimeExpired
  case object CartEmpty
  case class AddItem(item: Item)
  case class ItemAdded(item: Item)
  case class RemoveItem(item: Item)
  case class ItemRemoved(item: Item)
}


class Cart(customer: ActorRef) extends Actor with Timers {
  import Cart._

  val items: ListBuffer[Item] = ListBuffer[Item]()

  private def startTimer(): Unit = {
    timers.startSingleTimer(TimerKey, CartTimeExpired, 3.second)
  }

  private def becomeEmpty(): Unit = {
    customer ! CartEmpty
    context become empty
  }

  private def becomeNonEmpty(): Unit = {
    startTimer()
    context become nonEmpty
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      items.append(item)
      sender ! ItemAdded(item)
      becomeNonEmpty()
  }

  def nonEmpty: Receive = LoggingReceive {

    case CartTimeExpired =>
      items.clear()
      becomeEmpty()

    case AddItem(item) =>
      items.append(item)
      sender ! ItemAdded(item)

    case RemoveItem(item) if items.length > 1 =>
      items -= item
      sender ! ItemRemoved(item)

    case RemoveItem(item) if items.length == 1 =>
      items -= item
      sender ! ItemRemoved(item)
      becomeEmpty()

    case StartCheckout =>
      val checkout = createCheckout()
      customer ! CheckoutStarted(checkout)
      context become inCheckout
  }

  def createCheckout(): ActorRef = {
    context.actorOf(Props(new Checkout(customer, self)), "checkout")
  }

  def inCheckout: Receive = LoggingReceive {
    case CheckoutClosed =>
      items.clear()
      becomeEmpty()

    case CheckoutCancelled =>
      becomeNonEmpty()
  }

  def receive: Receive = empty
}
