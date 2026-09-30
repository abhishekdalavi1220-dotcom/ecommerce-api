package com.abhishek.ecommerce.controller;

import com.abhishek.ecommerce.dto.CheckoutResponse;
import com.abhishek.ecommerce.dto.OrderResponse;
import com.abhishek.ecommerce.model.User;
import com.abhishek.ecommerce.service.OrderService;
import com.abhishek.ecommerce.service.StripeService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final StripeService stripeService;

    /** Converts the current cart into an order and starts a Stripe PaymentIntent. */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.checkout(user));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal User user) {
        List<OrderResponse> orders = orderService.getUserOrders(user).stream().map(OrderResponse::from).toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrder(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(OrderResponse.from(orderService.getUserOrder(user, id)));
    }

    /**
     * Stripe webhook receiver — permitted without auth in SecurityConfig, but every request
     * is cryptographically verified against the raw body + Stripe-Signature header before
     * anything in it is trusted. Configure this URL in the Stripe Dashboard / CLI.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                        @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event = stripeService.constructEvent(payload, sigHeader);

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                orderService.markOrderPaid(intent.getId());
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                orderService.markOrderFailed(intent.getId());
            }
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("received");
    }
}
