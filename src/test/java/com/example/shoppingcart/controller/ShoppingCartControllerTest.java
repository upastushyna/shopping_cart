package com.example.shoppingcart.controller;

import com.example.shoppingcart.dto.CartItemRequest;
import com.example.shoppingcart.dto.ShoppingCartResponse;
import com.example.shoppingcart.exception.ResourceNotFoundException;
import com.example.shoppingcart.model.Product;
import com.example.shoppingcart.model.ShoppingCart;
import com.example.shoppingcart.service.ShoppingCartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = ShoppingCartController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
class ShoppingCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockBean
    private ShoppingCartService shoppingCartService;

    private Product product1;
    private ShoppingCart activeCart;
    private ShoppingCartResponse activeCartResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Ensure dates are formatted as ISO strings

        product1 = Product.builder().id(1L).name("Laptop").price(new BigDecimal("1000.00")).type("ELECTRONICS").build();

        activeCart = ShoppingCart.builder()
                .id(100L)
                .status(ShoppingCart.CartStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .lastModifiedAt(LocalDateTime.now().minusDays(1))
                .build();
        activeCart.getItems().add(com.example.shoppingcart.model.CartItem.builder()
                .id(1L).shoppingCart(activeCart).product(product1).quantity(1).build());

        activeCartResponse = ShoppingCartResponse.fromEntity(activeCart);
    }

    @Test
    void createCart_shouldReturnNewCart_andStatus201() throws Exception {
        ShoppingCart newCart = ShoppingCart.builder()
                .id(1L)
                .status(ShoppingCart.CartStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();
        when(shoppingCartService.createCart()).thenReturn(newCart);
        mockMvc.perform(post("/api/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(shoppingCartService, times(1)).createCart();
    }

    @Test
    void getCartById_shouldReturnCart_andStatus200() throws Exception {
        when(shoppingCartService.getCartById(100L)).thenReturn(activeCart);
        mockMvc.perform(get("/api/carts/{cartId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalPrice").value(1000.00)); // Price from product1 in activeCart

        verify(shoppingCartService, times(1)).getCartById(100L);
    }

    @Test
    void getCartById_shouldReturnNotFound_whenCartDoesNotExist() throws Exception {
        when(shoppingCartService.getCartById(999L)).thenThrow(new ResourceNotFoundException("Cart not found"));
        mockMvc.perform(get("/api/carts/{cartId}", 999L))
                .andExpect(status().isNotFound());

        verify(shoppingCartService, times(1)).getCartById(999L);
    }

    @Test
    void addItemToCart_shouldReturnUpdatedCart_andStatus200() throws Exception {
        CartItemRequest request = CartItemRequest.builder().productId(2L).quantity(2).build();
        ShoppingCart updatedCartEntity = ShoppingCart.builder()
                .id(100L)
                .status(ShoppingCart.CartStatus.ACTIVE)
                .createdAt(activeCart.getCreatedAt())
                .lastModifiedAt(LocalDateTime.now())
                .build();
        updatedCartEntity.getItems().add(com.example.shoppingcart.model.CartItem.builder()
                .id(1L).shoppingCart(updatedCartEntity).product(product1).quantity(1).build());
        updatedCartEntity.getItems().add(com.example.shoppingcart.model.CartItem.builder()
                .id(2L).shoppingCart(updatedCartEntity).product(Product.builder().id(2L).name("Mouse").price(new BigDecimal("25.00")).type("ELECTRONICS").build()).quantity(2).build());

        when(shoppingCartService.addItemToCart(eq(100L), any(CartItemRequest.class))).thenReturn(updatedCartEntity);
        mockMvc.perform(post("/api/carts/{cartId}/items", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[1].productName").value("Mouse"))
                .andExpect(jsonPath("$.items[1].quantity").value(2));

        verify(shoppingCartService, times(1)).addItemToCart(eq(100L), any(CartItemRequest.class));
    }

    @Test
    void addItemToCart_shouldReturnBadRequest_whenInvalidRequest() throws Exception {
        CartItemRequest invalidRequest = CartItemRequest.builder().productId(1L).quantity(0).build();
        mockMvc.perform(post("/api/carts/{cartId}/items", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(shoppingCartService, never()).addItemToCart(anyLong(), any(CartItemRequest.class));
    }

    @Test
    void removeItemFromCart_shouldReturnUpdatedCart_andStatus200() throws Exception {
        ShoppingCart emptyCartEntity = ShoppingCart.builder()
                .id(100L)
                .status(ShoppingCart.CartStatus.ACTIVE)
                .createdAt(activeCart.getCreatedAt())
                .lastModifiedAt(LocalDateTime.now())
                .items(new java.util.ArrayList<>()) // Empty list after removal
                .build();
        when(shoppingCartService.removeItemFromCart(100L, 1L, 1)).thenReturn(emptyCartEntity);
        mockMvc.perform(delete("/api/carts/{cartId}/items/{productId}", 100L, 1L)
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.items.length()").value(0));
        verify(shoppingCartService, times(1)).removeItemFromCart(100L, 1L, 1);
    }

    @Test
    void calculateTotalPrice_shouldReturnTotal_andStatus200() throws Exception {
        when(shoppingCartService.calculateTotalPrice(100L)).thenReturn(new BigDecimal("1000.00"));
        mockMvc.perform(get("/api/carts/{cartId}/total", 100L))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00")); // Direct BigDecimal string representation
        verify(shoppingCartService, times(1)).calculateTotalPrice(100L);
    }

    @Test
    void checkoutCart_shouldReturnCheckedOutCart_andStatus200() throws Exception {
        ShoppingCart checkedOutCartEntity = ShoppingCart.builder()
                .id(100L)
                .status(ShoppingCart.CartStatus.CHECKED_OUT)
                .createdAt(activeCart.getCreatedAt())
                .lastModifiedAt(LocalDateTime.now())
                .checkedOutAt(LocalDateTime.now())
                .build();
        checkedOutCartEntity.getItems().add(com.example.shoppingcart.model.CartItem.builder()
                .id(1L).shoppingCart(checkedOutCartEntity).product(product1).quantity(1).build());
        when(shoppingCartService.checkoutCart(100L)).thenReturn(checkedOutCartEntity);
        mockMvc.perform(post("/api/carts/{cartId}/checkout", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.checkedOutAt").exists());
        verify(shoppingCartService, times(1)).checkoutCart(100L);
    }

    @Test
    void getAbandonedCartsReport_shouldReturnListOfCarts_andStatus200() throws Exception {
        LocalDate reportDate = LocalDate.of(2023, 1, 1);
        ShoppingCart abandonedCart1 = ShoppingCart.builder().id(201L).status(ShoppingCart.CartStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2022, 12, 31, 10, 0)).build();
        ShoppingCart abandonedCart2 = ShoppingCart.builder().id(202L).status(ShoppingCart.CartStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2023, 1, 1, 8, 0)).build();
        when(shoppingCartService.getAbandonedCartsForReport(reportDate))
                .thenReturn(Arrays.asList(abandonedCart1, abandonedCart2));
        mockMvc.perform(get("/api/carts/report/abandoned")
                        .param("date", "2023-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(201L))
                .andExpect(jsonPath("$[1].id").value(202L));
        verify(shoppingCartService, times(1)).getAbandonedCartsForReport(reportDate);
    }

    @Test
    void printAbandonedCartsReportToConsole_shouldReturnOk_andStatus200() throws Exception {
        LocalDate reportDate = LocalDate.of(2023, 1, 1);
        doNothing().when(shoppingCartService).printReport(reportDate);
        mockMvc.perform(get("/api/carts/report/abandoned/print")
                        .param("date", "2023-01-01"))
                .andExpect(status().isOk())
                .andExpect(content().string("Report printed to console for 2023-01-01"));
        verify(shoppingCartService, times(1)).printReport(reportDate);
    }
}
