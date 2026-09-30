package com.abhishek.ecommerce.dto;

import com.abhishek.ecommerce.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;

    public static CartItemResponse from(CartItem item) {
        BigDecimal unitPrice = item.getProduct().getPrice();
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
