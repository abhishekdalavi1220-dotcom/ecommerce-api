package com.abhishek.ecommerce.service;

import com.abhishek.ecommerce.exception.BadRequestException;
import com.abhishek.ecommerce.exception.ResourceNotFoundException;
import com.abhishek.ecommerce.model.Cart;
import com.abhishek.ecommerce.model.CartItem;
import com.abhishek.ecommerce.model.Product;
import com.abhishek.ecommerce.model.User;
import com.abhishek.ecommerce.repository.CartItemRepository;
import com.abhishek.ecommerce.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    @Transactional
    public Cart addItem(User user, Long productId, int quantity) {
        Product product = productService.getById(productId);
        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException("Only " + product.getStockQuantity() + " unit(s) of \"" + product.getName() + "\" left in stock");
        }

        Cart cart = getOrCreateCart(user);

        cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .ifPresentOrElse(
                        existing -> {
                            int newQty = existing.getQuantity() + quantity;
                            if (product.getStockQuantity() < newQty) {
                                throw new BadRequestException("Only " + product.getStockQuantity() + " unit(s) of \"" + product.getName() + "\" left in stock");
                            }
                            existing.setQuantity(newQty);
                            cartItemRepository.save(existing);
                        },
                        () -> {
                            CartItem item = CartItem.builder()
                                    .cart(cart)
                                    .product(product)
                                    .quantity(quantity)
                                    .build();
                            cart.getItems().add(item);
                            cartItemRepository.save(item);
                        });

        return cartRepository.findById(cart.getId()).orElseThrow();
    }

    @Transactional
    public Cart updateItemQuantity(User user, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findOwnedItem(cart, itemId);

        if (item.getProduct().getStockQuantity() < quantity) {
            throw new BadRequestException("Only " + item.getProduct().getStockQuantity() + " unit(s) left in stock");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return cartRepository.findById(cart.getId()).orElseThrow();
    }

    @Transactional
    public Cart removeItem(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findOwnedItem(cart, itemId);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return cartRepository.findById(cart.getId()).orElseThrow();
    }

    @Transactional
    public Cart clear(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        return cartRepository.save(cart);
    }

    private CartItem findOwnedItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
    }
}
