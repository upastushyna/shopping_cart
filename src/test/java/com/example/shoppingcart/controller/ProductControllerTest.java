package com.example.shoppingcart.controller;

import com.example.shoppingcart.dto.ProductRequest;
import com.example.shoppingcart.exception.ResourceNotFoundException;
import com.example.shoppingcart.model.Product;
import com.example.shoppingcart.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = ProductController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private Product laptop;
    private ProductRequest laptopRequest;

    @BeforeEach
    void setUp() {

        laptop = Product.builder()
                .id(1L)
                .name("Laptop X")
                .price(new BigDecimal("1200.00"))
                .type("LAPTOP")
                .build();

        laptopRequest = ProductRequest.builder()
                .name("Laptop X")
                .price(new BigDecimal("1200.00"))
                .type("LAPTOP")
                .build();
    }

    @Test
    void createProduct_shouldReturnCreatedProduct_andStatus201() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(laptop);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptopRequest)))
                .andExpect(status().isCreated()) // Expect HTTP 201 Created
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Laptop X"))
                .andExpect(jsonPath("$.price").value(1200.00))
                .andExpect(jsonPath("$.type").value("LAPTOP"));

        verify(productService, times(1)).createProduct(any(ProductRequest.class));
    }

    @Test
    void createProduct_shouldReturnBadRequest_whenInvalidRequest() throws Exception {
        ProductRequest invalidRequest = ProductRequest.builder()
                .price(new BigDecimal("500.00"))
                .type("INVALID")
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // Expect HTTP 400 Bad Request

        verify(productService, never()).createProduct(any(ProductRequest.class));
    }

    @Test
    void getProductById_shouldReturnProduct_andStatus200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(laptop);

        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Laptop X"));

        verify(productService, times(1)).getProductById(1L);
    }

    @Test
    void getProductById_shouldReturnNotFound_whenProductDoesNotExist() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/{id}", 99L))
                .andExpect(status().isNotFound()); // Expect HTTP 404 Not Found

        verify(productService, times(1)).getProductById(99L);
    }

    @Test
    void getAllProducts_shouldReturnListOfProducts_andStatus200() throws Exception {
        Product phone = Product.builder().id(2L).name("Smartphone Y").price(new BigDecimal("800.00")).type("PHONE").build();
        List<Product> products = Arrays.asList(laptop, phone);
        when(productService.getAllProducts()).thenReturn(products);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Laptop X"))
                .andExpect(jsonPath("$[1].name").value("Smartphone Y"));

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    void updateProduct_shouldReturnUpdatedProduct_andStatus200() throws Exception {
        ProductRequest updatedRequest = ProductRequest.builder()
                .name("Laptop X Updated")
                .price(new BigDecimal("1300.00"))
                .type("LAPTOP")
                .build();
        Product updatedProduct = Product.builder()
                .id(1L)
                .name("Laptop X Updated")
                .price(new BigDecimal("1300.00"))
                .type("LAPTOP")
                .build();

        when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(updatedProduct);

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRequest)))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Laptop X Updated"))
                .andExpect(jsonPath("$.price").value(1300.00));

        verify(productService, times(1)).updateProduct(eq(1L), any(ProductRequest.class));
    }

    @Test
    void updateProduct_shouldReturnNotFound_whenProductDoesNotExist() throws Exception {
        when(productService.updateProduct(eq(99L), any(ProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product not found"));
        mockMvc.perform(put("/api/products/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptopRequest)))
                .andExpect(status().isNotFound()); // Expect HTTP 404 Not Found

        verify(productService, times(1)).updateProduct(eq(99L), any(ProductRequest.class));
    }

    @Test
    void deleteProduct_shouldReturnNoContent_andStatus204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);
        mockMvc.perform(delete("/api/products/{id}", 1L))
                .andExpect(status().isNoContent()); // Expect HTTP 204 No Content

        verify(productService, times(1)).deleteProduct(1L);
    }

    @Test
    void deleteProduct_shouldReturnNotFound_whenProductDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found")).when(productService).deleteProduct(99L);
        mockMvc.perform(delete("/api/products/{id}", 99L))
                .andExpect(status().isNotFound()); // Expect HTTP 404 Not Found

        verify(productService, times(1)).deleteProduct(99L);
    }
}