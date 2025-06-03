package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.ProductRequest;
import com.example.shoppingcart.exception.ResourceNotFoundException;
import com.example.shoppingcart.model.Product;
import com.example.shoppingcart.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ProductService class.
 * Uses Mockito to mock the ProductRepository dependency.
 */
@ExtendWith(MockitoExtension.class) // Enables Mockito annotations
class ProductServiceTest {

    @Mock // Mocks the ProductRepository
    private ProductRepository productRepository;

    @InjectMocks // Injects the mocked repository into ProductService
    private ProductService productService;

    private Product laptop;
    private ProductRequest laptopRequest;

    @BeforeEach
    void setUp() {
        // Initialize common test data before each test method
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
    void createProduct_shouldReturnCreatedProduct() {
        // Given: a product request and a mocked repository save method
        when(productRepository.save(any(Product.class))).thenReturn(laptop);

        // When: createProduct method is called
        Product createdProduct = productService.createProduct(laptopRequest);

        // Then: verify the product was saved and returned correctly
        assertNotNull(createdProduct);
        assertEquals(laptop.getName(), createdProduct.getName());
        assertEquals(laptop.getPrice(), createdProduct.getPrice());
        assertEquals(laptop.getType(), createdProduct.getType());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getProductById_shouldReturnProduct_whenFound() {
        // Given: a product ID and a mocked repository findById method
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        // When: getProductById method is called
        Product foundProduct = productService.getProductById(1L);

        // Then: verify the correct product is returned
        assertNotNull(foundProduct);
        assertEquals(laptop.getId(), foundProduct.getId());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductById_shouldThrowResourceNotFoundException_whenNotFound() {
        // Given: a product ID and a mocked repository findById method returning empty
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        // When/Then: verify that ResourceNotFoundException is thrown
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(2L));
        verify(productRepository, times(1)).findById(2L);
    }

    @Test
    void getAllProducts_shouldReturnListOfProducts() {
        // Given: a list of products and a mocked repository findAll method
        Product phone = Product.builder().id(2L).name("Phone Y").price(new BigDecimal("800.00")).type("PHONE").build();
        List<Product> products = Arrays.asList(laptop, phone);
        when(productRepository.findAll()).thenReturn(products);

        // When: getAllProducts method is called
        List<Product> foundProducts = productService.getAllProducts();

        // Then: verify the list of products is returned
        assertNotNull(foundProducts);
        assertEquals(2, foundProducts.size());
        assertTrue(foundProducts.contains(laptop));
        assertTrue(foundProducts.contains(phone));
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void updateProduct_shouldReturnUpdatedProduct_whenFound() {
        // Given: an updated product request and a mocked repository
        ProductRequest updatedRequest = ProductRequest.builder()
                .name("Laptop X Pro")
                .price(new BigDecimal("1300.00"))
                .type("LAPTOP")
                .build();
        Product updatedProduct = Product.builder()
                .id(1L)
                .name("Laptop X Pro")
                .price(new BigDecimal("1300.00"))
                .type("LAPTOP")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // When: updateProduct method is called
        Product result = productService.updateProduct(1L, updatedRequest);

        // Then: verify the product was updated and returned correctly
        assertNotNull(result);
        assertEquals(updatedRequest.getName(), result.getName());
        assertEquals(updatedRequest.getPrice(), result.getPrice());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(laptop); // Verify save was called on the modified 'laptop' object
    }

    @Test
    void updateProduct_shouldThrowResourceNotFoundException_whenNotFound() {
        // Given: a non-existent product ID
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        // When/Then: verify that ResourceNotFoundException is thrown
        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(2L, laptopRequest));
        verify(productRepository, times(1)).findById(2L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_shouldDeleteProduct_whenFound() {
        // Given: a product ID and a mocked repository existsById method
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        // When: deleteProduct method is called
        productService.deleteProduct(1L);

        // Then: verify deleteById was called
        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteProduct_shouldThrowResourceNotFoundException_whenNotFound() {
        // Given: a non-existent product ID
        when(productRepository.existsById(2L)).thenReturn(false);

        // When/Then: verify that ResourceNotFoundException is thrown
        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(2L));
        verify(productRepository, times(1)).existsById(2L);
        verify(productRepository, never()).deleteById(anyLong());
    }
}