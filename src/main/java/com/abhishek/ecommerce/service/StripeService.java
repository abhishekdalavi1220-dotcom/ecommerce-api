package com.abhishek.ecommerce.service;

import com.abhishek.ecommerce.config.StripeConfig;
import com.abhishek.ecommerce.exception.BadRequestException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeConfig stripeConfig;

    /**
     * Creates a Stripe PaymentIntent for the given order total.
     * Amount must be converted to the smallest currency unit (e.g. cents for USD).
     */
    public PaymentIntent createPaymentIntent(BigDecimal amount, Long orderId) {
        long amountInMinorUnits = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInMinorUnits)
                .setCurrency(stripeConfig.getCurrency())
                .putMetadata("orderId", String.valueOf(orderId))
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .build();

        try {
            return PaymentIntent.create(params);
        } catch (StripeException e) {
            throw new BadRequestException("Could not initiate payment: " + e.getMessage());
        }
    }

    /** Verifies the Stripe-Signature header and parses the webhook payload into an Event. */
    public Event constructEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new BadRequestException("Invalid Stripe webhook signature");
        }
    }
}
