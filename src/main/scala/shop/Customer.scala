package shop

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive

object Customer {
  case object Init
  case object DoPayment
}

class Customer extends Actor {

  import Customer._

  val cart: ActorRef = context.actorOf(Props(new Cart(self)), "cart")

  def receive: Receive = LoggingReceive {

    case Init =>
      val watch = new Item("zegarek")
      val shoes = new Item("buty")
      val game = new Item("game")

      cart ! Cart.AddItem(watch)
      cart ! Cart.RemoveItem(watch)
      cart ! Cart.AddItem(shoes)
      cart ! Cart.AddItem(watch)
      cart ! Cart.RemoveItem(watch)
      cart ! Cart.AddItem(game)

      cart ! Cart.StartCheckout

    case Cart.CheckoutStarted(checkout: ActorRef) =>
      checkout ! Checkout.DeliveryMethodSelected("dhl")
      checkout ! Checkout.PaymentSelected("visa")

    case Cart.CheckoutClosed =>
      print("Customer: checkout closed!")

    case Cart.CartEmpty =>
      print("Customer: cart is empty!")

    case Checkout.PaymentServiceStarted(payment: ActorRef) =>
      payment ! DoPayment

    case PaymentService.PaymentConfirmed =>
      print("Customer: payment confirmed!")
  }
}
