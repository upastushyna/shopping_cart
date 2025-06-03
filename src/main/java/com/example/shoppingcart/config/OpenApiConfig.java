package com.example.shoppingcart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Springdoc OpenAPI (Swagger UI).
 * Defines basic API information like title, version, and description.
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Online Shopping Cart API")
                        .version("1.0")
                        .description("API for managing products, shopping carts, and generating end-of-day reports."));
    }
}
