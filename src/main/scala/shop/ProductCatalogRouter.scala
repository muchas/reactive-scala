package shop;

import akka.actor.{Actor, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import shop.ProductCatalogManager.SearchRequest


class ProductCatalogRouter extends Actor {

  private def createCatalogManager() = {
    context.actorOf(Props(new ProductCatalogManager("./query_result") ))
  }

  var router = {
    val routees = Vector.fill(3) {
      val r = createCatalogManager()
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case w: SearchRequest ⇒
      router.route(w, sender())
    case Terminated(a) ⇒
      router = router.removeRoutee(a)
      val r = createCatalogManager()
      context watch r
      router = router.addRoutee(r)
  }
}