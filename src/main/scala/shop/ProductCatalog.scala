package shop

import java.net.URI

case class ProductCatalog(items: Map[URI, (Item, Int)]) {

    def addItem(it: Item, count: Int): ProductCatalog = {
        val currentCount = if (items contains it.id) items(it.id)._2 else 0
        copy(items = items.updated(it.id, (it, currentCount + count) ))
    }

    def removeItem(id: URI, count: Int): ProductCatalog = {
        val item = if (items contains id) items(id)._1 else null
        val newCount = if (items contains id) Math.max(items(id)._2 - count, 0) else 0

        if(newCount <= 0) {
            copy(items = items - id)
        } else if (item != null) {
            copy(items = items.updated(id, (item, newCount)))
        } else {
            copy(items)
        }
    }

    def itemsCount(): Int = items.values.map(x => x._2).sum

    def itemsList(): Seq[Item] = items.values.map(x => x._1).toList

    private def countInKeywords(item: Item, query: String): Int = {
        val words = query.split(" ").map(_.toLowerCase)

        words
          .toStream
          .map(x => (item.name contains x, item.brand contains x))
          .map(x => if(x._1 && x._2) 2 else if(x._1 || x._2) 1 else 0)
          .sum
    }

    def search(query: String, limit: Int): List[Item] =
        items.values
          .toStream
          .filter(_._2 > 0)
          .map(_._1)
          .sortBy(-countInKeywords(_, query))
          .take(limit)
          .toList
}

object ProductCatalog {
  val empty = ProductCatalog(Map.empty)

  def fromCSV(path: String): ProductCatalog = {
      var catalog = ProductCatalog.empty
      val items = scala.io.Source.fromFile(path)
        .getLines
        .toStream
        .tail
        .map(_.split(",").map(_.replace("\"", "")))
        .filter(_.length > 2)
        .map(
            x => Item(
              URI.create(x(0)),
              x(1).trim,
              10,
              x(2).trim)
          )
        .filterNot(x => x.brand contains "NULL")

    for(item <- items) catalog = catalog.addItem(item, 1)

    catalog
  }
}

