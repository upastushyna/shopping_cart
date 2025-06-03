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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ShoppingCartService class.
 * Uses Mockito to mock repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private ShoppingCartService shoppingCartService;

    private Product product1;
    private Product product2;
    private ShoppingCart activeCart;
    private CartItem cartItem1;

    @BeforeEach
    void setUp() {
        product1 = Product.builder().id(1L).name("Laptop").price(new BigDecimal("1000.00")).type("ELECTRONICS").build();
        product2 = Product.builder().id(2L).name("Mouse").price(new BigDecimal("25.00")).type("ELECTRONICS").build();

        activeCart = ShoppingCart.builder()
                .id(100L)
                .status(CartStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .lastModifiedAt(LocalDateTime.now().minusDays(1))
                .items(new ArrayList<>()) // Initialize with an empty list
                .build();

        cartItem1 = CartItem.builder()
                .id(1L)
                .shoppingCart(activeCart)
                .product(product1)
                .quantity(1)
                .build();
        activeCart.getItems().add(cartItem1); // Add item to cart's list
    }

    @Test
    void createCart_shouldReturnNewShoppingCart() {
        // Given
        ShoppingCart newCart = new ShoppingCart();
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(newCart);

        // When
        ShoppingCart createdCart = shoppingCartService.createCart();

        // Then
        assertNotNull(createdCart);
        assertEquals(CartStatus.ACTIVE, createdCart.getStatus());
        assertTrue(createdCart.getItems().isEmpty());
        verify(shoppingCartRepository, times(1)).save(any(ShoppingCart.class));
    }

    @Test
    void getCartById_shouldReturnCart_whenFound() {
        // Given
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));

        // When
        ShoppingCart foundCart = shoppingCartService.getCartById(100L);

        // Then
        assertNotNull(foundCart);
        assertEquals(activeCart.getId(), foundCart.getId());
        verify(shoppingCartRepository, times(1)).findById(100L);
    }

    @Test
    void getCartById_shouldThrowResourceNotFoundException_whenNotFound() {
        // Given
        when(shoppingCartRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ResourceNotFoundException.class, () -> shoppingCartService.getCartById(999L));
        verify(shoppingCartRepository, times(1)).findById(999L);
    }

    @Test
    void addItemToCart_shouldAddNewItem_whenProductNotInCart() {
        // Given
        CartItemRequest request = CartItemRequest.builder().productId(2L).quantity(2).build();
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(cartItemRepository.findByShoppingCartAndProduct(activeCart, product2)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the saved item
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(activeCart);

        // When
        ShoppingCart updatedCart = shoppingCartService.addItemToCart(100L, request);

        // Then
        assertNotNull(updatedCart);
        assertEquals(2, updatedCart.getItems().size()); // Original item + new item
        assertTrue(updatedCart.getItems().stream().anyMatch(item -> item.getProduct().equals(product2) && item.getQuantity() == 2));
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
        verify(shoppingCartRepository, times(1)).save(activeCart);
    }

    @Test
    void addItemToCart_shouldUpdateExistingItemQuantity_whenProductAlreadyInCart() {
        // Given
        CartItemRequest request = CartItemRequest.builder().productId(1L).quantity(2).build(); // Add more of product1
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(cartItemRepository.findByShoppingCartAndProduct(activeCart, product1)).thenReturn(Optional.of(cartItem1));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the saved item
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(activeCart);


        // When
        ShoppingCart updatedCart = shoppingCartService.addItemToCart(100L, request);

        // Then
        assertNotNull(updatedCart);
        assertEquals(1, updatedCart.getItems().size()); // Still one item, but quantity updated
        assertEquals(3, updatedCart.getItems().get(0).getQuantity()); // Original 1 + added 2 = 3
        verify(cartItemRepository, times(1)).save(cartItem1);
        verify(shoppingCartRepository, times(1)).save(activeCart);
    }

    @Test
    void addItemToCart_shouldThrowIllegalStateException_whenCartCheckedOut() {
        // Given
        activeCart.setStatus(CartStatus.CHECKED_OUT);
        CartItemRequest request = CartItemRequest.builder().productId(1L).quantity(1).build();
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));

        // When / Then
        assertThrows(IllegalStateException.class, () -> shoppingCartService.addItemToCart(100L, request));
        verify(productRepository, never()).findById(anyLong());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void removeItemFromCart_shouldReduceQuantity_whenQuantityToRemoveIsLess() {
        // Given
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(cartItemRepository.findByShoppingCartAndProduct(activeCart, product1)).thenReturn(Optional.of(cartItem1));
        cartItem1.setQuantity(5); // Set initial quantity for testing reduction
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(activeCart);

        // When
        ShoppingCart updatedCart = shoppingCartService.removeItemFromCart(100L, 1L, 2);

        // Then
        assertNotNull(updatedCart);
        assertEquals(1, updatedCart.getItems().size());
        assertEquals(3, updatedCart.getItems().get(0).getQuantity()); // 5 - 2 = 3
        verify(cartItemRepository, times(1)).save(cartItem1);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
        verify(shoppingCartRepository, times(1)).save(activeCart);
    }

    @Test
    void removeItemFromCart_shouldRemoveItemCompletely_whenQuantityToRemoveIsEqualOrMore() {
        // Given
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(cartItemRepository.findByShoppingCartAndProduct(activeCart, product1)).thenReturn(Optional.of(cartItem1));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(activeCart);
        doNothing().when(cartItemRepository).delete(cartItem1);

        // When
        ShoppingCart updatedCart = shoppingCartService.removeItemFromCart(100L, 1L, 1); // Remove exact quantity

        // Then
        assertNotNull(updatedCart);
        assertTrue(updatedCart.getItems().isEmpty()); // Item should be removed
        verify(cartItemRepository, times(1)).delete(cartItem1);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(shoppingCartRepository, times(1)).save(activeCart);
    }

    @Test
    void removeItemFromCart_shouldThrowResourceNotFoundException_whenItemNotInCart() {
        // Given
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));
        when(productRepository.findById(99L)).thenReturn(Optional.of(product2)); // Mock product exists
        when(cartItemRepository.findByShoppingCartAndProduct(activeCart, product2)).thenReturn(Optional.empty()); // But not in cart

        // When / Then
        assertThrows(ResourceNotFoundException.class, () -> shoppingCartService.removeItemFromCart(100L, 99L, 1));
        verify(cartItemRepository, never()).delete(any(CartItem.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void removeItemFromCart_shouldThrowIllegalStateException_whenCartCheckedOut() {
        // Given
        activeCart.setStatus(CartStatus.CHECKED_OUT);
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));

        // When / Then
        assertThrows(IllegalStateException.class, () -> shoppingCartService.removeItemFromCart(100L, 1L, 1));
        verify(productRepository, never()).findById(anyLong());
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void calculateTotalPrice_shouldReturnCorrectTotal() {
        // Given
        Product p3 = Product.builder().id(3L).name("Keyboard").price(new BigDecimal("75.50")).type("ACCESSORY").build();
        CartItem ci2 = CartItem.builder().shoppingCart(activeCart).product(p3).quantity(2).build();
        activeCart.getItems().add(ci2); // Add another item

        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));

        // When
        BigDecimal totalPrice = shoppingCartService.calculateTotalPrice(100L);

        // Then
        // (1 * 1000.00) + (2 * 75.50) = 1000.00 + 151.00 = 1151.00
        assertEquals(new BigDecimal("1151.00"), totalPrice);
        verify(shoppingCartRepository, times(1)).findById(100L);
    }

    @Test
    void calculateTotalPrice_shouldReturnZero_whenCartEmpty() {
        // Given
        ShoppingCart emptyCart = ShoppingCart.builder()
                .id(101L)
                .status(CartStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        when(shoppingCartRepository.findById(101L)).thenReturn(Optional.of(emptyCart));

        // When
        BigDecimal totalPrice = shoppingCartService.calculateTotalPrice(101L);

        // Then
        assertEquals(BigDecimal.ZERO, totalPrice);
    }

    @Test
    void checkoutCart_shouldChangeStatusAndSetTimestamp() {
        // Given
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ShoppingCart checkedOutCart = shoppingCartService.checkoutCart(100L);

        // Then
        assertNotNull(checkedOutCart);
        assertEquals(CartStatus.CHECKED_OUT, checkedOutCart.getStatus());
        assertNotNull(checkedOutCart.getCheckedOutAt());
        assertTrue(checkedOutCart.getCheckedOutAt().isAfter(activeCart.getCreatedAt())); // Checked out time should be after creation
        verify(shoppingCartRepository, times(1)).save(activeCart);
    }

    @Test
    void checkoutCart_shouldThrowIllegalStateException_whenAlreadyCheckedOut() {
        // Given
        activeCart.setStatus(CartStatus.CHECKED_OUT);
        when(shoppingCartRepository.findById(100L)).thenReturn(Optional.of(activeCart));

        // When / Then
        assertThrows(IllegalStateException.class, () -> shoppingCartService.checkoutCart(100L));
        verify(shoppingCartRepository, never()).save(any(ShoppingCart.class));
    }

    @Test
    void getAbandonedCartsForReport_shouldReturnCorrectCarts() {
        // Given
        LocalDate reportDate = LocalDate.of(2023, 10, 26);
        LocalDateTime endOfDay = reportDate.atTime(LocalTime.MAX);

        ShoppingCart abandonedCart1 = ShoppingCart.builder()
                .id(201L).status(CartStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2023, 10, 25, 10, 0))
                .lastModifiedAt(LocalDateTime.of(2023, 10, 25, 10, 0))
                .items(new ArrayList<>())
                .build();
        abandonedCart1.getItems().add(CartItem.builder().shoppingCart(abandonedCart1).product(product1).quantity(1).build());

        ShoppingCart abandonedCart2 = ShoppingCart.builder()
                .id(202L).status(CartStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2023, 10, 26, 8, 0))
                .lastModifiedAt(LocalDateTime.of(2023, 10, 26, 8, 0))
                .items(new ArrayList<>())
                .build();
        abandonedCart2.getItems().add(CartItem.builder().shoppingCart(abandonedCart2).product(product2).quantity(2).build());

        ShoppingCart checkedOutCart = ShoppingCart.builder()
                .id(203L).status(CartStatus.CHECKED_OUT)
                .createdAt(LocalDateTime.of(2023, 10, 25, 12, 0))
                .lastModifiedAt(LocalDateTime.of(2023, 10, 25, 12, 0))
                .checkedOutAt(LocalDateTime.of(2023, 10, 25, 15, 0))
                .items(new ArrayList<>())
                .build();

        // Mock the repository call
        when(shoppingCartRepository.findByStatusAndCreatedAtBeforeAndCheckedOutAtIsNull(
                CartStatus.ACTIVE, endOfDay))
                .thenReturn(Arrays.asList(abandonedCart1, abandonedCart2));

        // When
        List<ShoppingCart> result = shoppingCartService.getAbandonedCartsForReport(reportDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(abandonedCart1));
        assertTrue(result.contains(abandonedCart2));
        assertFalse(result.contains(checkedOutCart)); // Checked out cart should not be in abandoned report
        verify(shoppingCartRepository, times(1)).findByStatusAndCreatedAtBeforeAndCheckedOutAtIsNull(
                CartStatus.ACTIVE, endOfDay);
    }

    @Test
    void printReport_shouldPrintToConsole() {
        // Given
        LocalDate reportDate = LocalDate.of(2023, 10, 26);
        ShoppingCart abandonedCart = ShoppingCart.builder()
                .id(201L).status(CartStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2023, 10, 25, 10, 0))
                .lastModifiedAt(LocalDateTime.of(2023, 10, 25, 10, 0))
                .items(new ArrayList<>())
                .build();
        abandonedCart.getItems().add(CartItem.builder().shoppingCart(abandonedCart).product(product1).quantity(1).build());

        when(shoppingCartRepository.findByStatusAndCreatedAtBeforeAndCheckedOutAtIsNull(
                eq(CartStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(abandonedCart));

        // When
        // Redirect System.out to capture console output
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        shoppingCartService.printReport(reportDate);

        // Then
        String output = outContent.toString();
        assertTrue(output.contains("--- Abandoned Carts Report for 2023-10-26 ---"));
        assertTrue(output.contains("Cart ID: 201"));
        assertTrue(output.contains("- Laptop (ID: 1), Quantity: 1, Price: $1000.00, Item Total: $1000.00"));
        assertTrue(output.contains("--- End of Report ---"));

        // Reset System.out
        System.setOut(System.out);
    }

    @Test
    void printReport_shouldPrintNoCartsFound_whenEmpty() {
        // Given
        LocalDate reportDate = LocalDate.of(2023, 10, 26);
        when(shoppingCartRepository.findByStatusAndCreatedAtBeforeAndCheckedOutAtIsNull(
                eq(CartStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        shoppingCartService.printReport(reportDate);

        // Then
        String output = outContent.toString();
        assertTrue(output.contains("No abandoned carts found for 2023-10-26."));

        // Reset System.out
        System.setOut(System.out);
    }
}