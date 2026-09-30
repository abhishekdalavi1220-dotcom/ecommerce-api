package com.abhishek.ecommerce.controller;

import com.abhishek.ecommerce.dto.CartItemRequest;
import com.abhishek.ecommerce.dto.CartResponse;
import com.abhishek.ecommerce.dto.UpdateQuantityRequest;
import com.abhishek.ecommerce.model.User;
import com.abhishek.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(CartResponse.from(cartService.getOrCreateCart(user)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal User user,
                                                 @Valid @RequestBody CartItemRequest request) {
        var cart = cartService.addItem(user, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(@AuthenticationPrincipal User user,
                                                    @PathVariable Long itemId,
                                                    @Valid @RequestBody UpdateQuantityRequest request) {
        var cart = cartService.updateItemQuantity(user, itemId, request.getQuantity());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal User user,
                                                    @PathVariable Long itemId) {
        var cart = cartService.removeItem(user, itemId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal User user) {
        var cart = cartService.clear(user);
        return ResponseEntity.ok(CartResponse.from(cart));
    }
}
