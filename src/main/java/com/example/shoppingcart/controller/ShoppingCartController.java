package com.example.shoppingcart.controller;


import com.example.shoppingcart.dto.CartItemRequest;
import com.example.shoppingcart.dto.ShoppingCartResponse;
import com.example.shoppingcart.model.ShoppingCart;
import com.example.shoppingcart.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/carts")
@Tag(name = "Shopping Carts", description = "API for managing shopping carts and reports")
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @Autowired
    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @Operation(summary = "Create a new shopping cart")
    @PostMapping
    public ResponseEntity<ShoppingCartResponse> createCart() {
        ShoppingCart newCart = shoppingCartService.createCart();
        return new ResponseEntity<>(ShoppingCartResponse.fromEntity(newCart), HttpStatus.CREATED);
    }

    @Operation(summary = "Get a shopping cart by ID")
    @GetMapping("/{cartId}")
    public ResponseEntity<ShoppingCartResponse> getCartById(@PathVariable Long cartId) {
        ShoppingCart cart = shoppingCartService.getCartById(cartId);
        return ResponseEntity.ok(ShoppingCartResponse.fromEntity(cart));
    }

    @Operation(summary = "Add an item to a shopping cart")
    @PostMapping("/{cartId}/items")
    public ResponseEntity<ShoppingCartResponse> addItemToCart(
            @PathVariable Long cartId,
            @Valid @RequestBody CartItemRequest request) {
        ShoppingCart updatedCart = shoppingCartService.addItemToCart(cartId, request);
        return ResponseEntity.ok(ShoppingCartResponse.fromEntity(updatedCart));
    }

    @Operation(summary = "Remove an item from a shopping cart")
    @DeleteMapping("/{cartId}/items/{productId}")
    public ResponseEntity<ShoppingCartResponse> removeItemFromCart(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @Parameter(description = "Quantity to remove. If not specified, removes all of this product.", example = "1")
            @RequestParam(required = false, defaultValue = "2147483647") int quantity) { // Max int to remove all if not specified
        ShoppingCart updatedCart = shoppingCartService.removeItemFromCart(cartId, productId, quantity);
        return ResponseEntity.ok(ShoppingCartResponse.fromEntity(updatedCart));
    }

    @Operation(summary = "Calculate total price of a shopping cart")
    @GetMapping("/{cartId}/total")
    public ResponseEntity<BigDecimal> calculateTotalPrice(@PathVariable Long cartId) {
        BigDecimal totalPrice = shoppingCartService.calculateTotalPrice(cartId);
        return ResponseEntity.ok(totalPrice);
    }

    @Operation(summary = "Checkout a shopping cart")
    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<ShoppingCartResponse> checkoutCart(@PathVariable Long cartId) {
        ShoppingCart checkedOutCart = shoppingCartService.checkoutCart(cartId);
        return ResponseEntity.ok(ShoppingCartResponse.fromEntity(checkedOutCart));
    }

    @Operation(summary = "Generate a report of abandoned shopping carts for a given date")
    @GetMapping("/report/abandoned")
    public ResponseEntity<List<ShoppingCartResponse>> getAbandonedCartsReport(
            @Parameter(description = "Date for the report (YYYY-MM-DD)", example = "2023-10-26")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ShoppingCart> abandonedCarts = shoppingCartService.getAbandonedCartsForReport(date);
        List<ShoppingCartResponse> responses = abandonedCarts.stream()
                .map(ShoppingCartResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Print report of abandoned carts to console (demonstration only)")
    @GetMapping("/report/abandoned/print")
    public ResponseEntity<String> printAbandonedCartsReportToConsole(
            @Parameter(description = "Date for the report (YYYY-MM-DD)", example = "2023-10-26")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        shoppingCartService.printReport(date);
        return ResponseEntity.ok("Report printed to console for " + date);
    }
}
