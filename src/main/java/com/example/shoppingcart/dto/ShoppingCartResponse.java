package com.example.shoppingcart.dto;


import com.example.shoppingcart.model.ShoppingCart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingCartResponse {
    private Long id;
    private ShoppingCart.CartStatus status;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime checkedOutAt;


    public static ShoppingCartResponse fromEntity(ShoppingCart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartItemResponse::fromEntity)
                .collect(Collectors.toList());

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ShoppingCartResponse.builder()
                .id(cart.getId())
                .status(cart.getStatus())
                .items(itemResponses)
                .totalPrice(total)
                .createdAt(cart.getCreatedAt())
                .lastModifiedAt(cart.getLastModifiedAt())
                .checkedOutAt(cart.getCheckedOutAt())
                .build();
    }
}
