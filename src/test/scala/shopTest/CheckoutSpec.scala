package shopTest

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}
import shop._


class CheckoutSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with ImplicitSender {

  var customer: TestProbe = _
  var cart: TestProbe = _
  var paymentService: ActorRef = _
  var checkout: ActorRef = _

  override def beforeEach(): Unit = {
    customer = TestProbe()
    cart = TestProbe()
    paymentService = TestProbe().ref
    checkout = system.actorOf(Props(new Checkout(customer.ref, cart.ref) {
      override def createPayment(): ActorRef = paymentService
    }))
  }

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Checkout" must {

    "inform about cart close" in {
      customer.send(checkout, Checkout.DeliveryMethodSelected("dhl"))
      customer.send(checkout, Checkout.PaymentSelected("paypal"))
      customer.expectMsg(Checkout.PaymentServiceStarted(paymentService))
      checkout ! Checkout.PaymentReceived
      cart.expectMsg(Cart.CheckoutClosed)
    }

    "inform about cart cancel when checkout cancelled" in {
      customer.send(checkout, Checkout.DeliveryMethodSelected("dhl"))
      customer.send(checkout, Checkout.PaymentSelected("paypal"))
      customer.expectMsg(Checkout.PaymentServiceStarted(paymentService))
      checkout ! Checkout.Cancelled
      cart.expectMsg(Cart.CheckoutCancelled)
    }

    "inform about cart cancel when payment expired" in {
      customer.send(checkout, Checkout.DeliveryMethodSelected("dhl"))
      customer.send(checkout, Checkout.PaymentSelected("paypal"))
      customer.expectMsg(Checkout.PaymentServiceStarted(paymentService))
      checkout ! Checkout.PaymentTimerExpired
      cart.expectMsg(Cart.CheckoutCancelled)
    }

    "inform about cart cancel when checkout expired" in {
      customer.send(checkout, Checkout.DeliveryMethodSelected("dhl"))
      checkout ! Checkout.CheckoutTimerExpired
      cart.expectMsg(Cart.CheckoutCancelled)
    }
  }
}
