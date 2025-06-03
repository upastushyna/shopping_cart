package com.example.shoppingcart.repository;

import com.example.shoppingcart.model.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    List<ShoppingCart> findByStatusAndCreatedAtBeforeAndCheckedOutAtIsNull(ShoppingCart.CartStatus status, LocalDateTime date);

}
