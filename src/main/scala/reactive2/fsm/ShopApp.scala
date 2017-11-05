package reactive2.fsm

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Customer {
  case object StartShopping
}

class Customer extends Actor {

  import Customer._

  val cart = context.actorOf(Props[Cart], "cart")

  def receive: Receive = LoggingReceive {

    case StartShopping =>
      val watch = new Item("zegarek")
      val shoes = new Item("buty")
      val game = new Item("game")

      cart ! ItemAdded(watch)
      cart ! ItemRemoved(watch)
      cart ! ItemAdded(shoes)

      cart ! ItemAdded(watch)
      cart ! ItemRemoved(watch)

      cart ! ItemAdded(game)
      cart ! CheckoutStarted

    case CheckoutCreated(checkout: ActorRef) =>
      checkout ! DeliveryMethodSelected("dhl")
      checkout ! PaymentSelected("visa")
      checkout ! PaymentReceived
  }
}



object ShopApp extends App {
  val system = ActorSystem("Reactive2")
  val mainActor = system.actorOf(Props[Customer], "mainActor")

  mainActor ! Customer.StartShopping

  Await.result(system.whenTerminated, Duration.Inf)
}