package com.abhishek.ecommerce.service;

import com.abhishek.ecommerce.dto.CheckoutResponse;
import com.abhishek.ecommerce.exception.BadRequestException;
import com.abhishek.ecommerce.exception.ResourceNotFoundException;
import com.abhishek.ecommerce.model.*;
import com.abhishek.ecommerce.repository.OrderRepository;
import com.abhishek.ecommerce.repository.ProductRepository;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final StripeService stripeService;

    /**
     * Turns the user's current cart into a PENDING order and starts a Stripe PaymentIntent.
     * Stock is re-validated here (it may have changed since items were added to the cart)
     * but is only decremented once payment is confirmed via webhook.
     */
    @Transactional
    public CheckoutResponse checkout(User user) {
        Cart cart = cartService.getOrCreateCart(user);

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout an empty cart");
        }

        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Only " + product.getStockQuantity() + " unit(s) of \"" + product.getName() + "\" left in stock");
            }
            return OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build();
        }).toList();

        BigDecimal total = orderItems.stream()
                .map(i -> i.getPriceAtPurchase().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .build();
       // orderItems.forEach(item -> item.setOrder(order));
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setItems(orderItems);
        order = orderRepository.save(order);

        PaymentIntent intent = stripeService.createPaymentIntent(total, order.getId());
        order.setStripePaymentIntentId(intent.getId());
        order.setStripeClientSecret(intent.getClientSecret());
        order = orderRepository.save(order);

        return CheckoutResponse.builder()
                .orderId(order.getId())
                .clientSecret(intent.getClientSecret())
                .status(order.getStatus().name())
                .build();
    }

    /** Called by the Stripe webhook when a PaymentIntent succeeds: marks the order PAID, decrements stock, clears the cart. */
    @Transactional
    public void markOrderPaid(String paymentIntentId) {
        Order order = orderRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("No order for PaymentIntent: " + paymentIntentId));

        if (order.getStatus() == OrderStatus.PAID) {
            return; // idempotent: Stripe may send the same event more than once
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int remaining = product.getStockQuantity() - item.getQuantity();
            product.setStockQuantity(Math.max(remaining, 0));
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        cartService.clear(order.getUser());
    }

    /** Called by the Stripe webhook when a PaymentIntent fails. */
    @Transactional
    public void markOrderFailed(String paymentIntentId) {
        orderRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(order -> {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
        });
    }

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Order getUserOrder(User user, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
