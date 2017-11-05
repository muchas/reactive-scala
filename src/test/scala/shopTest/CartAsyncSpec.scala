package shopTest

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}
import shop._


class CartAsyncSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with ImplicitSender {

  var customer: TestProbe = _
  var checkout: ActorRef = _
  var item: Item = _
  var cart: ActorRef = _

  override def beforeEach(): Unit = {
      customer = TestProbe()
      checkout = TestProbe().ref
      item = new Item("example")
      cart = system.actorOf(Props(new Cart(customer.ref) {
        override def createCheckout(): ActorRef = checkout
      }))
  }

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Cart" must {

    "add item" in {
      cart ! Cart.AddItem(item)

      expectMsg(Cart.ItemAdded(item))
    }

    "remove item" in {
      cart ! Cart.AddItem(item)
      expectMsg(Cart.ItemAdded(item))
      cart ! Cart.RemoveItem(item)
      expectMsg(Cart.ItemRemoved(item))
    }

    "inform about checkout start" in {
      cart ! Cart.AddItem(item)
      cart ! Cart.StartCheckout
      customer.expectMsg(Cart.CheckoutStarted(checkout))
    }

    "inform about empty cart" in {
      cart ! Cart.AddItem(item)
      cart ! Cart.RemoveItem(item)
      customer.expectMsg(Cart.CartEmpty)
    }

    "inform about empty cart when checkout closed" in {
      cart ! Cart.AddItem(item)
      cart ! Cart.StartCheckout
      customer.expectMsg(Cart.CheckoutStarted(checkout))
      cart ! Cart.CheckoutClosed
      customer.expectMsg(Cart.CartEmpty)
    }

    "inform about empty cart when timer expired" in {
      cart ! Cart.AddItem(item)
      cart ! Cart.CartTimeExpired
      customer.expectMsg(Cart.CartEmpty)
    }
  }
}
