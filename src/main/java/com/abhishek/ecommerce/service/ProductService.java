package com.abhishek.ecommerce.service;

import com.abhishek.ecommerce.dto.ProductRequest;
import com.abhishek.ecommerce.exception.ResourceNotFoundException;
import com.abhishek.ecommerce.model.Product;
import com.abhishek.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Page<Product> search(String keyword, String category, Pageable pageable) {
        return productRepository.search(
                (keyword == null || keyword.isBlank()) ? null : keyword,
                (category == null || category.isBlank()) ? null : category,
                pageable);
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .build();
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = getById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateStock(Long id, Integer newQuantity) {
        Product product = getById(id);
        product.setStockQuantity(newQuantity);
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }
}
