package shop

import akka.actor.Actor
import akka.event.LoggingReceive

object ProductCatalogManager {

  // Protocol
  case class AddItemRequest(item: Item, quantity: Int = 1)
  case class AddItemResponse(item: Item, quantity: Int = 1)

  case class RemoveItemRequest(item: Item, quantity: Int = 1)
  case class RemoveItemResponse(item: Item, quantity: Int = 1)

  case class SearchRequest(query: String, limit: Int = 10)
  case class SearchResponse(items: Seq[Item])
}


class ProductCatalogManager(var productCatalog: ProductCatalog) extends Actor {
  import ProductCatalogManager._

  def this(path: String) {
    this(ProductCatalog.fromCSV(path))
  }

  override def receive: Receive = LoggingReceive {
    case AddItemRequest(item, quantity) =>
      productCatalog = productCatalog.addItem(item, quantity)
      sender ! AddItemResponse(item, quantity)
    case RemoveItemRequest(item, quantity) =>
      productCatalog = productCatalog.removeItem(item.id, quantity)
      sender ! RemoveItemResponse(item, quantity)
    case SearchRequest(query, limit) =>
      val items = productCatalog.search(query, limit)
      sender ! SearchResponse(items)
  }
}
