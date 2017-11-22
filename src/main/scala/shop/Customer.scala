package shop

import java.net.URI

import akka.actor.{Actor, ActorRef, ActorSelection, Props}
import akka.event.LoggingReceive

object Customer {
  case object Init
  case object Search
  case object DoPayment
}

class Customer extends Actor {

  import Customer._

  val cart: ActorRef = context.actorOf(Props(new CartManager(self)), "cart")
  val catalog: ActorSelection = context.system.actorSelection("akka.tcp://catalogSystem@127.0.0.1:2553/user/productCatalogManager")

  def receive: Receive = LoggingReceive {

    // Search Queries
    case Search => catalog ! ProductCatalogManager.SearchRequest("fanta", 12)
    case ProductCatalogManager.SearchResponse(items) => items.foreach(println)


    // Shop Queries
    case Init =>
      val watch = Item(URI.create("1"), "zegarek", 5.12)
      val shoes = Item(URI.create("2"), "buty", 1.12)
      val game = Item(URI.create("3"), "kapcie", 6.20)

      cart ! CartManager.AddItem(watch, 1)
      cart ! CartManager.RemoveItem(watch, 1)
      cart ! CartManager.AddItem(shoes, 1)
      cart ! CartManager.AddItem(watch, 1)
      cart ! CartManager.RemoveItem(watch, 1)
      cart ! CartManager.AddItem(game, 1)

      cart ! CartManager.StartCheckout

    case CartManager.CheckoutStarted(checkout: ActorRef) =>
      checkout ! Checkout.DeliveryMethodSelected("dhl")
      checkout ! Checkout.PaymentSelected("visa")

    case CartManager.CheckoutClosed =>
      print("Customer: checkout closed!")

    case CartManager.CartEmpty =>
      print("Customer: cart is empty!")

    case Checkout.PaymentServiceStarted(payment: ActorRef) =>
      payment ! DoPayment

    case PaymentService.PaymentConfirmed =>
      print("Customer: payment confirmed!")
  }
}
