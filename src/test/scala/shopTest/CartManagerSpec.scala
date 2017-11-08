package shopTest

import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import shop._


class CartManagerSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Cart" must {

    "add item" in {

      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new CartManager(customer))
      val item = Item(URI.create("example"), "name", 1)

      cart ! CartManager.AddItem(item, 1)

      assert (cart.underlyingActor.shoppingCart.items.size == 1)
//      assert (cart.underlyingActor.shoppingCart.head == item)

    }

    "remove item" in {

      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new CartManager(customer))
      val item = Item(URI.create("example"), "name", 1)

      cart ! CartManager.AddItem(item, 1)
      cart ! CartManager.RemoveItem(item, 1)

      assert (cart.underlyingActor.shoppingCart.items.isEmpty)
    }

    "add multiple items" in {
      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new CartManager(customer))
      val item = Item(URI.create("example"), "name", 1)
      val item2 = Item(URI.create("example"), "name", 1)
      val item3 = Item(URI.create("example"), "name", 1)

      cart ! CartManager.AddItem(item, 1)
      cart ! CartManager.AddItem(item2, 1)
      cart ! CartManager.AddItem(item3, 1)

      assert (cart.underlyingActor.shoppingCart.items.size == 3)
//      assert (cart.underlyingActor.items.head == item)

    }

    "remove multiple items" in {
      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new CartManager(customer))
      val item = Item(URI.create("example"), "name", 1)
      val item2 = Item(URI.create("example"), "name", 1)
      val item3 = Item(URI.create("example"), "name", 1)
      val item4 = Item(URI.create("example"), "name", 1)

      cart ! CartManager.AddItem(item, 1)
      cart ! CartManager.AddItem(item2, 1)
      cart ! CartManager.AddItem(item3, 1)
      cart ! CartManager.AddItem(item4, 1)
      cart ! CartManager.RemoveItem(item, 1)
      cart ! CartManager.RemoveItem(item3, 1)

      assert (cart.underlyingActor.shoppingCart.items.size == 2)
//      assert (cart.underlyingActor.items.head == item2)
    }

    "allow removal of specified item" in {
      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new CartManager(customer))
      val item = Item(URI.create("example"), "name", 1)
      val item2 = Item(URI.create("example"), "name", 1)

      cart ! CartManager.AddItem(item, 1)
      cart ! CartManager.RemoveItem(item2, 1)

      assert (cart.underlyingActor.shoppingCart.items.size == 1)
//      assert (cart.underlyingActor.items.head == item)
    }
  }
}
