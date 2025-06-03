package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.CartItemRequest;
import com.example.shoppingcart.exception.ResourceNotFoundException;
import com.example.shoppingcart.model.CartItem;
import com.example.shoppingcart.model.Product;
import com.example.shoppingcart.model.ShoppingCart;
import com.example.shoppingcart.model.ShoppingCart.CartStatus;
import com.example.shoppingcart.repository.CartItemRepository;
import com.example.shoppingcart.repository.ProductRepository;
import com.example.shoppingcart.repository.ShoppingCartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;


@Service
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    @Autowired
    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository,
                               ProductRepository productRepository,
                               CartItemRepository cartItemRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public ShoppingCart createCart() {
        ShoppingCart cart = new ShoppingCart();
        return shoppingCartRepository.save(cart);
    }

    public ShoppingCart getCartById(Long cartId) {
        return shoppingCartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart not found with ID: " + cartId));
    }

    @Transactional
    public ShoppingCart addItemToCart(Long cartId, CartItemRequest request) {
        ShoppingCart cart = getCartById(cartId);
        if (cart.getStatus() == CartStatus.CHECKED_OUT) {
            throw new IllegalStateException("Cannot add items to a checked out cart.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        Optional<CartItem> existingCartItem = cartItemRepository.findByShoppingCartAndProduct(cart, product);

        if (existingCartItem.isPresent()) {
            CartItem item = existingCartItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .shoppingCart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }
        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public ShoppingCart removeItemFromCart(Long cartId, Long productId, int quantityToRemove) {
        ShoppingCart cart = getCartById(cartId);
        if (cart.getStatus() == CartStatus.CHECKED_OUT) {
            throw new IllegalStateException("Cannot remove items from a checked out cart.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        CartItem cartItem = cartItemRepository.findByShoppingCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found in cart " + cartId));

        if (cartItem.getQuantity() <= quantityToRemove) {
            cart.getItems().remove(cartItem); // Remove from cart's items list
            cartItemRepository.delete(cartItem); // Delete from database
        } else {
            cartItem.setQuantity(cartItem.getQuantity() - quantityToRemove);
            cartItemRepository.save(cartItem);
        }
        return shoppingCartRepository.save(cart); // Save cart to update lastModifiedAt
    }

    public BigDecimal calculateTotalPrice(Long cartId) {
        ShoppingCart cart = getCartById(cartId);
        return cart.getItems().stream()
                .map(CartItem::getItemTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public ShoppingCart checkoutCart(Long cartId) {
        ShoppingCart cart = getCartById(cartId);
        if (cart.getStatus() == CartStatus.CHECKED_OUT) {
            throw new IllegalStateException("Shopping cart with ID " + cartId + " is already checked out.");
        }
        cart.setStatus(CartStatus.CHECKED_OUT);
        cart.setCheckedOutAt(LocalDateTime.now());

        return shoppingCartRepository.save(cart);
    }

    public List<ShoppingCart> getAbandonedCartsForReport(LocalDate date) {
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return shoppingCartRepository.findByStatusAndCreatedAtBeforeAndCheckedOutAtIsNull(
                CartStatus.ACTIVE, endOfDay);
    }

    public void printReport(LocalDate date) {
        System.out.println("\n--- Abandoned Carts Report for " + date + " ---");
        List<ShoppingCart> abandonedCarts = getAbandonedCartsForReport(date);

        if (abandonedCarts.isEmpty()) {
            System.out.println("No abandoned carts found for " + date + ".");
            return;
        }

        abandonedCarts.forEach(cart -> {
            System.out.println("Cart ID: " + cart.getId());
            System.out.println("  Created At: " + cart.getCreatedAt());
            System.out.println("  Items:");
            cart.getItems().forEach(item -> {
                System.out.println("    - " + item.getProduct().getName() + " (ID: " + item.getProduct().getId() +
                        "), Quantity: " + item.getQuantity() +
                        ", Price: $" + item.getProduct().getPrice() +
                        ", Item Total: $" + item.getItemTotalPrice());
            });
            System.out.println("------------------------------------");
        });
        System.out.println("--- End of Report ---");
    }
}
