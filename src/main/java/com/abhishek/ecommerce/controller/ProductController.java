package com.abhishek.ecommerce.controller;

import com.abhishek.ecommerce.dto.ProductResponse;
import com.abhishek.ecommerce.model.Product;
import com.abhishek.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** Public browsing/search endpoint: GET /api/products?keyword=phone&category=electronics&page=0&size=20 */
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductResponse> page = productService.search(keyword, category, pageable)
                .map(ProductResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }
}
