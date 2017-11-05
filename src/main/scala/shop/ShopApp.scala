package shop

import akka.actor.{ActorSystem, Props}

import scala.concurrent.Await
import scala.concurrent.duration.Duration



object ShopApp extends App {
  val system = ActorSystem("Reactive2")
  val mainActor = system.actorOf(Props[Customer], "mainActor")

  mainActor ! Customer.Init

  Await.result(system.whenTerminated, Duration.Inf)
}