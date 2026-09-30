package com.abhishek.ecommerce.controller;

import com.abhishek.ecommerce.dto.ProductRequest;
import com.abhishek.ecommerce.dto.ProductResponse;
import com.abhishek.ecommerce.dto.UpdateStockRequest;
import com.abhishek.ecommerce.model.Product;
import com.abhishek.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Everything here requires ROLE_ADMIN — enforced in SecurityConfig via the /api/admin/** matcher. */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Product product = productService.update(id, request);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable Long id, @Valid @RequestBody UpdateStockRequest request) {
        Product product = productService.updateStock(id, request.getStockQuantity());
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
