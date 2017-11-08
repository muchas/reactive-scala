package shopTest

import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}
import shop._


class CartSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach {

  var item: Item = _
  var cart: Cart = Cart.empty

  override def beforeEach(): Unit = {
    cart = Cart.empty
    item = Item(URI.create("example"), "name", 1)
  }

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Cart" must {

    "add item" in {
      cart = cart.addItem(item, 1)

      assert (cart.items.size == 1)
      assert (cart.itemsCount() == 1)
      assert {cart.items contains item.id}
    }

    "remove item" in {
      cart = cart.addItem(item, 1)
      cart = cart.removeItem(item.id, 1)

      assert (cart.items.size == 0)
      assert (cart.itemsCount() == 0)
      assert {!(cart.items contains item.id)}
    }

    "be empty at start" in {
      assert (cart.itemsCount() == 0)
    }

    "add multiple items" in {
      val item2 = Item(URI.create("example3"), "name", 1)
      val item3 = Item(URI.create("example4"), "name", 1)

      cart = cart.addItem(item, 1)
      cart = cart.addItem(item2, 1)
      cart = cart.addItem(item3, 1)

      assert (cart.items.size == 3)
      assert (cart.itemsCount() == 3)
    }

    "remove multiple items" in {
      val item2 = Item(URI.create("example3"), "name", 1)
      val item3 = Item(URI.create("example4"), "name", 1)

      cart = cart.addItem(item, 1)
      cart = cart.addItem(item2, 1)
      cart = cart.addItem(item3, 1)
      cart = cart.removeItem(item.id, 1)
      cart = cart.removeItem(item2.id, 1)

      assert (cart.items.size == 1)
      assert (cart.itemsCount() == 1)
      assert (!(cart.items contains item.id))
      assert (!(cart.items contains item2.id))
    }

    "add same item with count > 0" in {
      cart = cart.addItem(item, 4)

      assert (cart.items.size == 1)
      assert (cart.itemsCount() == 4)
    }

    "add same item multiple times" in {
      cart = cart.addItem(item, 4)
      cart = cart.addItem(item, 3)
      cart = cart.addItem(item, 2)

      assert (cart.items.size == 1)
      assert (cart.itemsCount() == 9)
    }

    "add many items multiple times" in {
      val item2 = Item(URI.create("example3"), "name", 1)
      val item3 = Item(URI.create("example4"), "name", 1)

      cart = cart.addItem(item, 4)
      cart = cart.addItem(item2, 3)
      cart = cart.addItem(item3, 2)

      assert (cart.items.size == 3)
      assert (cart.itemsCount() == 9)
    }

    "remove only added number of items" in {
      cart = cart.addItem(item, 1)
      cart = cart.removeItem(item.id, 5)

      assert (cart.items.size == 0)
      assert (cart.itemsCount() == 0)
    }

    "tolerate removal of non-existent item" in {
      cart = cart.addItem(item, 1)
      cart = cart.removeItem(URI.create("unknownId"), 10)

      assert (cart.items.size == 1)
      assert (cart.itemsCount() == 1)
    }
  }
}
