package shopTest

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}
import shop._


class CheckoutSpec extends TestKit(ActorSystem("CheckoutSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with ImplicitSender {

  var customer: TestProbe = _
  var cart: TestProbe = _
  var paymentService: ActorRef = _
  var checkout: ActorRef = _

  private def createCheckoutActor(id: String): ActorRef = {
   system.actorOf(Props(new Checkout(customer.ref, cart.ref, id) {
      override def createPayment(): ActorRef = paymentService
    }))
  }

  override def beforeEach(): Unit = {
    customer = TestProbe()
    cart = TestProbe()
    paymentService = TestProbe().ref
  }

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Checkout" must {

    "inform about cart close" in {
      checkout = createCheckoutActor("checkout-test-01")
      val deliveryMethod = "dhl"
      customer.send(checkout, Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.expectMsg(Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.send(checkout, Checkout.PaymentSelected("paypal"))
      customer.expectMsg(Checkout.PaymentServiceStarted(paymentService))
      checkout ! Checkout.PaymentReceived
      cart.expectMsg(CartManager.CheckoutClosed)
    }

    "inform about cart cancel when checkout cancelled" in {
      checkout = createCheckoutActor("checkout-test-02")
      val deliveryMethod = "dhl"
      customer.send(checkout, Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.expectMsg(Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.send(checkout, Checkout.PaymentSelected("paypal"))
      customer.expectMsg(Checkout.PaymentServiceStarted(paymentService))
      checkout ! Checkout.Cancelled
      cart.expectMsg(CartManager.CheckoutCancelled)
    }

    "inform about cart cancel when payment expired" in {
      checkout = createCheckoutActor("checkout-test-03")
      val deliveryMethod = "dhl"
      customer.send(checkout, Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.expectMsg(Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.send(checkout, Checkout.PaymentSelected("paypal"))
      customer.expectMsg(Checkout.PaymentServiceStarted(paymentService))
      checkout ! Checkout.PaymentTimerExpired
      cart.expectMsg(CartManager.CheckoutCancelled)
    }

    "inform about cart cancel when checkout expired" in {
      checkout = createCheckoutActor("checkout-test-04")
      val deliveryMethod = "dhl"
      customer.send(checkout, Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.expectMsg(Checkout.DeliveryMethodSelected(deliveryMethod))
      checkout ! Checkout.CheckoutTimerExpired
      cart.expectMsg(CartManager.CheckoutCancelled)
    }

    "select delivery method and and preserve it after restart" in {
      val checkoutId = "checkout-test-010"
      val deliveryMethod = "dhl"

      checkout = createCheckoutActor(checkoutId)

      customer.send(checkout, Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.expectMsg(Checkout.DeliveryMethodSelected(deliveryMethod))

      checkout ! PoisonPill

      val checkout2 = createCheckoutActor(checkoutId)

      checkout2 ! Checkout.DeliveryMethodRequest
      expectMsg(Checkout.DeliveryMethodResponse(deliveryMethod))

      checkout2 ! Checkout.StateRequest
      expectMsg(Checkout.StateResponse(Checkout.SelectingPaymentMethod(0)))

    }

    "select payment method and and preserve it after restart" in {
      val checkoutId = "checkout-test-011"
      val deliveryMethod = "dhl"
      val paymentMethod = "stripe"

      checkout = createCheckoutActor(checkoutId)

      customer.send(checkout, Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.expectMsg(Checkout.DeliveryMethodSelected(deliveryMethod))
      customer.send(checkout, Checkout.PaymentSelected(paymentMethod))
      customer.expectMsg(Checkout.PaymentServiceStarted(paymentService))

      checkout ! PoisonPill

      val checkout2 = createCheckoutActor(checkoutId)

      checkout2 ! Checkout.DeliveryMethodRequest
      expectMsg(Checkout.DeliveryMethodResponse(deliveryMethod))

      checkout2 ! Checkout.PaymentMethodRequest
      expectMsg(Checkout.PaymentMethodResponse(paymentMethod))

      checkout2 ! Checkout.StateRequest
      expectMsg(Checkout.StateResponse(Checkout.ProcessingPayment(0)))
    }

  }
}
