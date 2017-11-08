package shopTest

import java.net.URI

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}
import shop._


class CartManagerAsyncSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with ImplicitSender {

  var customer: TestProbe = _
  var checkout: ActorRef = _
  var item: Item = _
  var cart: ActorRef = _

  override def beforeEach(): Unit = {
      customer = TestProbe()
      checkout = TestProbe().ref
      item = Item(URI.create("example"), "name", 1)
      cart = system.actorOf(Props(new CartManager(customer.ref) {
        override def createCheckout(): ActorRef = checkout
      }))
  }

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Cart" must {

    "add item" in {
      cart ! CartManager.AddItem(item, 1)

      expectMsg(CartManager.ItemAdded(item, 1))
    }

    "remove item" in {
      cart ! CartManager.AddItem(item, 1)
      expectMsg(CartManager.ItemAdded(item, 1))
      cart ! CartManager.RemoveItem(item, 1)
      expectMsg(CartManager.ItemRemoved(item, 1))
    }

    "inform about checkout start" in {
      cart ! CartManager.AddItem(item, 1)
      cart ! CartManager.StartCheckout
      customer.expectMsg(CartManager.CheckoutStarted(checkout))
    }

    "inform about empty cart" in {
      cart ! CartManager.AddItem(item, 1)
      cart ! CartManager.RemoveItem(item, 1)
      customer.expectMsg(CartManager.CartEmpty)
    }

    "inform about empty cart when checkout closed" in {
      cart ! CartManager.AddItem(item, 1)
      cart ! CartManager.StartCheckout
      customer.expectMsg(CartManager.CheckoutStarted(checkout))
      cart ! CartManager.CheckoutClosed
      customer.expectMsg(CartManager.CartEmpty)
    }

    "inform about empty cart when timer expired" in {
      cart ! CartManager.AddItem(item, 1)
      cart ! CartManager.CartTimeExpired
      customer.expectMsg(CartManager.CartEmpty)
    }
  }
}
