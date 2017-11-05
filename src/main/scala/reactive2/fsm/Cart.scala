package reactive2.fsm

import akka.actor.{ActorRef, FSM, Props}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._



final case class ItemAdded(item: Item)
final case class ItemRemoved(item: Item)
final case class CheckoutCreated(checkout: ActorRef)
case object CheckoutStarted
case object CheckoutClosed
case object CheckoutCancelled


sealed trait CartState
case object Empty extends CartState
case object NonEmpty extends CartState
case object InCheckout extends CartState


sealed trait CartData
case object None extends CartData
final case class ItemStore(checkout: ActorRef, items: ListBuffer[Item]) extends CartData


case class Item(name: String)


class Cart extends FSM[CartState, CartData] {

  var checkout: ActorRef = null

  startWith(Empty, None)

  when(Empty) {
    case Event(ItemAdded(item), None) =>
      goto(NonEmpty) using ItemStore(null, ListBuffer(item))
  }

  when(NonEmpty, stateTimeout = 5 seconds) {
    case Event(ItemAdded(item), store @ ItemStore(_, items)) =>
      items += item
      stay using ItemStore(null, items)

    case Event(ItemRemoved(item), store @ ItemStore(_, items)) if items.length > 1 =>
      items -= item
      stay using ItemStore(null, items)

    case Event(ItemRemoved(item), store @ ItemStore(_, items)) if items.length == 1 =>
      goto(Empty) using None

    case Event(CheckoutStarted, store @ ItemStore(_, items)) =>
      checkout = context.actorOf(Props(new Checkout(self)), "checkout")
      sender ! CheckoutCreated(checkout)
      goto(InCheckout) using ItemStore(checkout, items)

    case Event(StateTimeout, _) =>
      goto(Empty) using None
  }

  when(InCheckout) {
    case Event(CheckoutCancelled, store: ItemStore) =>
      goto(NonEmpty) using store

    case Event(CheckoutClosed, _) =>
      goto(Empty) using None
  }


  initialize()
}
