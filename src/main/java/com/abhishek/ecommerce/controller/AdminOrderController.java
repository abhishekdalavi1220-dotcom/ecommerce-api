package com.abhishek.ecommerce.controller;

import com.abhishek.ecommerce.dto.OrderResponse;
import com.abhishek.ecommerce.dto.UpdateOrderStatusRequest;
import com.abhishek.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Everything here requires ROLE_ADMIN — enforced in SecurityConfig via the /api/admin/** matcher. */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        Page<OrderResponse> page = orderService.getAllOrders(pageable).map(OrderResponse::from);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(OrderResponse.from(orderService.updateStatus(id, request.getStatus())));
    }
}
