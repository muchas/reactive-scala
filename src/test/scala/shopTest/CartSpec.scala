package shopTest

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import shop._


class CartSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Cart" must {

    "add item" in {

      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new Cart(customer))
      val item = new Item("example")

      cart ! Cart.AddItem(item)

      assert (cart.underlyingActor.items.length == 1)
      assert (cart.underlyingActor.items.head == item)

    }

    "remove item" in {

      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new Cart(customer))
      val item = new Item("example")

      cart ! Cart.AddItem(item)
      cart ! Cart.RemoveItem(item)

      assert (cart.underlyingActor.items.length == 0)
    }

    "add multiple items" in {
      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new Cart(customer))
      val item = new Item("example")
      val item2 = new Item("another")
      val item3 = new Item("test")

      cart ! Cart.AddItem(item)
      cart ! Cart.AddItem(item2)
      cart ! Cart.AddItem(item3)

      assert (cart.underlyingActor.items.length == 3)
      assert (cart.underlyingActor.items.head == item)

    }

    "remove multiple items" in {
      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new Cart(customer))
      val item = new Item("example")
      val item2 = new Item("another")
      val item3 = new Item("test")
      val item4 = new Item("test1")

      cart ! Cart.AddItem(item)
      cart ! Cart.AddItem(item2)
      cart ! Cart.AddItem(item3)
      cart ! Cart.AddItem(item4)
      cart ! Cart.RemoveItem(item)
      cart ! Cart.RemoveItem(item3)

      assert (cart.underlyingActor.items.length == 2)
      assert (cart.underlyingActor.items.head == item2)
    }

    "allow removal of specified item" in {
      val customer = TestActorRef[Customer]
      val cart = TestActorRef(new Cart(customer))
      val item = new Item("example")
      val item2 = new Item("another")

      cart ! Cart.AddItem(item)
      cart ! Cart.RemoveItem(item2)

      assert (cart.underlyingActor.items.length == 1)
      assert (cart.underlyingActor.items.head == item)
    }
  }
}
