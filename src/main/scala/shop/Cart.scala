package shop

import java.net.URI


case class Cart(items: Map[URI, (Item, Int)]) {

    def addItem(it: Item, count: Int): Cart = {
        val currentCount = if (items contains it.id) items(it.id)._2 else 0
        copy(items = items.updated(it.id, (it, currentCount + count) ))
    }

    def removeItem(id: URI, count: Int): Cart = {
        val item = if (items contains id) items(id)._1 else null
        val newCount = if (items contains id) Math.max(items(id)._2 - count, 0) else 0

        if (item != null) {
            copy(items = items.updated(id, (item, newCount)))
        } else if(newCount == 0) {
            copy(items = items - item.id)
        } else {
            copy(items)
        }
    }

    def itemsCount(): Int = {
        var sum = 0
        for ((_, (_, count)) <- items) sum += count
        sum
    }
}

object Cart {
  val empty = Cart(Map.empty)
}
