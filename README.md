# Shopping Cart API
This Spring Boot API manages an online shopping cart system, offering product management, cart operations, and abandoned cart reporting.

## Features
- Product Management: Create, retrieve, update, and delete products (name, price, type).
- Shopping Cart Operations: Create carts; add/remove products (updates quantity); calculate total price; checkout carts.
- Abandoned Cart Reporting: Generate reports for active, un-checked-out carts by a specified date.
- RESTful API: Clear, consistent interactions.
- API Documentation: Interactive Swagger UI.

## Technologies Used
- Java 17+
- Spring Boot 3.2.5 (Web, Data JPA)
- H2 Database (in-memory)
- Lombok
- Springdoc OpenAPI UI
- Maven
- JUnit 5 & Mockito

## Prerequisites
- Java Development Kit (JDK) 17+ (```java -version, javac -version```)
- Apache Maven 3.6.0+ (```mvn -v```)
Getting Started
 1. Project Setup: Organize files in a standard Maven structure.
 2. Build: Navigate to the project root and run ```mvn clean install```.
 3. Run: Execute ```mvn spring-boot:run ```or java ```-jar target/shopping-cart-0.0.1-SNAPSHOT.jar```. The app runs on ```http://localhost:8080```.
 4. Access API Docs: Visit http://localhost:8080/swagger-ui.html for interactive API testing.

## Database
Uses an in-memory H2 database for development; data is lost on restart. For production, configure a persistent database (e.g., PostgreSQL).

## Running Tests
From the project root, run ```mvn test``` to execute all unit and integration tests.

## Project Structure Overview
 Key directories: ```src/main/java/...``` (application code), ```src/main/resources/application.properties``` (configuration), ```src/test/java/...``` (test code).
