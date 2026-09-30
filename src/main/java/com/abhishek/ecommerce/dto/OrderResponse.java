package com.abhishek.ecommerce.dto;

import com.abhishek.ecommerce.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private Instant createdAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream().map(OrderItemResponse::from).toList())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
