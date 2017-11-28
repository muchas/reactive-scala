package shop

import java.net.URI


case class Item(id: URI, name: String, price: BigDecimal, brand: String = "")
