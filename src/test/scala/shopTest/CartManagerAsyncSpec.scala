package shopTest

import java.net.URI

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}
import shop.CartManager.GetItemsResponse
import shop._


class CartManagerAsyncSpec extends TestKit(ActorSystem("CartManagerAsyncSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with ImplicitSender {

  var customer: TestProbe = _
  var checkout: ActorRef = _
  var item: Item = _
  var cart: ActorRef = _

  private def createCartManagerActor(id: String): ActorRef = {
    system.actorOf(Props(new CartManager(customer.ref, id) {
        override def createCheckout(): ActorRef = checkout
    }))
  }

  override def beforeEach(): Unit = {
      customer = TestProbe()
      checkout = TestProbe().ref
      item = Item(URI.create("predefinedItem"), "name", 1)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Cart" must {

    "add item" in {
      cart = createCartManagerActor("ct-001")
      cart ! CartManager.AddItem(item, 1)

      expectMsg(CartManager.ItemAdded(item, 1))
    }

    "remove item" in {
      cart = createCartManagerActor("ct-002")

      cart ! CartManager.AddItem(item, 1)
      expectMsg(CartManager.ItemAdded(item, 1))
      cart ! CartManager.RemoveItem(item, 1)
      expectMsg(CartManager.ItemRemoved(item, 1))
    }

    "inform about checkout start" in {
      cart = createCartManagerActor("ct-003")

      cart ! CartManager.AddItem(item, 1)
      expectMsg(CartManager.ItemAdded(item, 1))

      cart ! CartManager.StartCheckout
      customer.expectMsg(CartManager.CheckoutStarted(checkout))
    }

    "inform about empty cart" in {
      cart = createCartManagerActor("ct-004")

      cart ! CartManager.AddItem(item, 1)
      expectMsg(CartManager.ItemAdded(item, 1))
      cart ! CartManager.RemoveItem(item, 1)
      expectMsg(CartManager.ItemRemoved(item, 1))
      customer.expectMsg(CartManager.CartEmpty)
    }

    "inform about empty cart when checkout closed" in {
      cart = createCartManagerActor("ct-005")

      cart ! CartManager.AddItem(item, 1)
      expectMsg(CartManager.ItemAdded(item, 1))

      cart ! CartManager.StartCheckout
      customer.expectMsg(CartManager.CheckoutStarted(checkout))
      cart ! CartManager.CheckoutClosed
      customer.expectMsg(CartManager.CartEmpty)
    }

    "inform about empty cart when timer expired" in {
      cart = createCartManagerActor("ct-006")

      cart ! CartManager.AddItem(item, 1)
      expectMsg(CartManager.ItemAdded(item, 1))

      cart ! CartManager.CartTimeExpired
      customer.expectMsg(CartManager.CartEmpty)
    }

    "add an item to the shopping cart and preserve it after restart" in {
      val cartManagerId = "ct-007"
      cart = createCartManagerActor(cartManagerId)

      cart ! CartManager.AddItem(item, 1)
      expectMsg(CartManager.ItemAdded(item, 1))

      cart ! PoisonPill

      val cart2 = createCartManagerActor(cartManagerId)

      cart2 ! CartManager.GetItemsRequest
      expectMsg(GetItemsResponse(Seq(item)))
    }
  }
}
