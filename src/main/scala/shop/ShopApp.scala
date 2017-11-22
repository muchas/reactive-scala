package shop

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration



object ShopApp extends App {
  val config = ConfigFactory.load()
  val catalogSystem = ActorSystem("catalogSystem", config.getConfig("catalog_app").withFallback(config))
  val system = ActorSystem("shopSystem", config.getConfig("shop_app").withFallback(config))

  val catalogActor = catalogSystem.actorOf(Props(new ProductCatalogManager("./query_result")), "productCatalogManager")
  val mainActor = system.actorOf(Props[Customer], "mainActor")

  mainActor ! Customer.Search

  Await.result(system.whenTerminated, Duration.Inf)
}
