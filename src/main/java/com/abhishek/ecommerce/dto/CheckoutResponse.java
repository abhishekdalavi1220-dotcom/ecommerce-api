package com.abhishek.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Returned to the client so it can complete payment with Stripe.js / Stripe Elements. */
@Data
@Builder
@AllArgsConstructor
public class CheckoutResponse {
    private Long orderId;
    private String clientSecret;
    private String status;
}
