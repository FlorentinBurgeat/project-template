# Real-World Examples - Kotlin/Spring Boot Backend

**CRITICAL REMINDERS:**
- **ALWAYS use UUID for IDs** ✅
- **ALWAYS use Instant (UTC) for timestamps** ✅
- Use hybrid architecture: simple features without domain layer, complex features with DDD patterns

---

## Table of Contents

1. [Simple Feature - User Authentication](#example-1-simple-feature---user-authentication)
2. [Complex Feature - Order Management with DDD](#example-2-complex-feature---order-management-with-ddd)
3. [Key Takeaways](#key-takeaways)

---

## Example 1: Simple Feature - User Authentication

**Architecture**: Simple feature without domain layer
**Structure**: `/features/authentication/` with controllers, services, models, repositories

**When to use**: CRUD operations, basic business logic, straightforward features

### Project Structure

```
/features/authentication/
├── controllers/       # AuthController.kt
├── services/          # AuthService.kt
├── models/            # User.kt, RegisterRequest.kt, AuthResponse.kt
├── repositories/      # UserRepository.kt
└── mappers/           # UserMapper.kt
```

### 1. Models (Simple Entities & DTOs)

```kotlin
// models/User.kt
package features.authentication.models

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID = UUID.randomUUID(),
    val email: String,
    val name: String,
    val passwordHash: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
```

```kotlin
// models/RegisterRequest.kt
package features.authentication.models

import javax.validation.constraints.*

data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    val name: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
)

data class LoginRequest(
    @field:NotBlank @field:Email
    val email: String,

    @field:NotBlank
    val password: String
)
```

```kotlin
// models/AuthResponse.kt
package features.authentication.models

import java.time.Instant
import java.util.UUID

data class AuthResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val createdAt: Instant
)

data class ErrorResponse(
    val message: String,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, String>? = null
)
```

### 2. Controller

```kotlin
// controllers/AuthController.kt
package features.authentication.controllers

import features.authentication.models.*
import features.authentication.services.AuthService
import org.springframework.http.*
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/api/auth")
@Validated
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val auth = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(auth)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val auth = authService.login(request)
        return ResponseEntity.ok(auth)
    }

    @GetMapping("/me")
    fun getCurrentUser(@RequestHeader("Authorization") token: String): ResponseEntity<UserResponse> {
        val user = authService.getCurrentUser(token)
        return ResponseEntity.ok(user)
    }
}
```

### 3. Service (Application Logic)

```kotlin
// services/AuthService.kt
package features.authentication.services

import features.authentication.models.*
import features.authentication.repositories.UserRepository
import features.authentication.mappers.toResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {

    fun register(request: RegisterRequest): AuthResponse {
        // Validate email not exists
        if (userRepository.findByEmail(request.email) != null) {
            throw EmailAlreadyInUseException("Email ${request.email} is already registered")
        }

        // Create user
        val user = User(
            id = UUID.randomUUID(),
            email = request.email,
            name = request.name,
            passwordHash = passwordEncoder.encode(request.password),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val savedUser = userRepository.save(user)

        // Generate JWT token
        val token = jwtProvider.generateToken(savedUser.id)

        return AuthResponse(
            token = token,
            user = savedUser.toResponse()
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val token = jwtProvider.generateToken(user.id)

        return AuthResponse(
            token = token,
            user = user.toResponse()
        )
    }

    fun getCurrentUser(authHeader: String): UserResponse {
        val token = authHeader.removePrefix("Bearer ")
        val userId = jwtProvider.getUserIdFromToken(token)
            ?: throw UnauthorizedException()

        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException(userId)

        return user.toResponse()
    }
}

// Custom exceptions
class EmailAlreadyInUseException(message: String) : RuntimeException(message)
class InvalidCredentialsException : RuntimeException("Invalid credentials")
class UnauthorizedException : RuntimeException("Unauthorized")
class UserNotFoundException(id: UUID) : RuntimeException("User $id not found")
```

### 4. Repository (Exposed ORM)

```kotlin
// repositories/UserRepository.kt
package features.authentication.repositories

import features.authentication.models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class UserRepository {

    fun save(user: User): User = transaction {
        Users.insert {
            it[id] = user.id
            it[email] = user.email
            it[name] = user.name
            it[passwordHash] = user.passwordHash
            it[createdAt] = user.createdAt
            it[updatedAt] = user.updatedAt
        }
        findById(user.id)!!
    }

    fun update(user: User): User = transaction {
        Users.update({ Users.id eq user.id }) {
            it[name] = user.name
            it[email] = user.email
            it[updatedAt] = Instant.now()
        }
        findById(user.id)!!
    }

    fun findById(id: UUID): User? = transaction {
        Users.select { Users.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        email = this[Users.email],
        name = this[Users.name],
        passwordHash = this[Users.passwordHash],
        createdAt = this[Users.createdAt],
        updatedAt = this[Users.updatedAt]
    )
}

// Table definition
object Users : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
```

### 5. Mapper (Extension Functions)

```kotlin
// mappers/UserMapper.kt
package features.authentication.mappers

import features.authentication.models.User
import features.authentication.models.UserResponse

fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    name = name,
    createdAt = createdAt
)
```

### 6. Database Migration

```sql
-- db/migration/V1__init_users.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);
```

### 7. Tests

```kotlin
// services/AuthServiceTest.kt
package features.authentication.services

import features.authentication.models.*
import features.authentication.repositories.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @InjectMocks
    private lateinit var authService: AuthService

    @Test
    fun `register should create user and return token`() {
        // Arrange
        val request = RegisterRequest(
            email = "john@example.com",
            name = "John Doe",
            password = "password123"
        )
        val userId = UUID.randomUUID()
        val now = Instant.now()
        val savedUser = User(userId, "john@example.com", "John Doe", "hashed", now, now)
        val token = "jwt.token.here"

        whenever(userRepository.findByEmail(request.email)).thenReturn(null)
        whenever(passwordEncoder.encode(request.password)).thenReturn("hashed")
        whenever(userRepository.save(any())).thenReturn(savedUser)
        whenever(jwtProvider.generateToken(userId)).thenReturn(token)

        // Act
        val result = authService.register(request)

        // Assert
        assertThat(result.token).isEqualTo(token)
        assertThat(result.user.email).isEqualTo("john@example.com")
        assertThat(result.user.name).isEqualTo("John Doe")
        verify(userRepository).save(any())
    }

    @Test
    fun `register should throw when email already exists`() {
        // Arrange
        val request = RegisterRequest("john@example.com", "John", "password123")
        val existingUser = User(
            UUID.randomUUID(),
            "john@example.com",
            "John",
            "hashed",
            Instant.now(),
            Instant.now()
        )
        whenever(userRepository.findByEmail(request.email)).thenReturn(existingUser)

        // Act & Assert
        assertThrows<EmailAlreadyInUseException> {
            authService.register(request)
        }
    }

    @Test
    fun `login should throw for invalid credentials`() {
        // Arrange
        val request = LoginRequest("john@example.com", "wrongpassword")
        val user = User(
            UUID.randomUUID(),
            "john@example.com",
            "John",
            "hashed",
            Instant.now(),
            Instant.now()
        )

        whenever(userRepository.findByEmail(request.email)).thenReturn(user)
        whenever(passwordEncoder.matches(request.password, "hashed")).thenReturn(false)

        // Act & Assert
        assertThrows<InvalidCredentialsException> {
            authService.login(request)
        }
    }
}
```

---

## Example 2: Complex Feature - Order Management with DDD

**Architecture**: Complex feature with DDD patterns (Aggregates, Value Objects, Entities)
**Structure**: `/features/order-management/` with domain layer

**When to use**: Complex business rules, multiple related entities, transaction boundaries, invariants to enforce

### Project Structure

```
/features/order-management/
├── controllers/       # OrderController.kt
├── services/          # OrderService.kt (orchestration)
├── domain/            # Business logic & rules
│   ├── aggregates/    # Order.kt (aggregate root)
│   ├── entities/      # OrderItem.kt
│   └── valueobjects/  # Money.kt, Address.kt, OrderStatus.kt
├── repositories/      # OrderRepository.kt (saves whole aggregate)
└── dto/               # CreateOrderRequest.kt, OrderResponse.kt
```

### 1. Value Objects (Immutable Concepts)

```kotlin
// domain/valueobjects/Money.kt
package features.ordermanagement.domain.valueobjects

import java.math.BigDecimal

data class Money(
    val amount: BigDecimal,
    val currency: String = "USD"
) {
    init {
        require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency != ${other.currency}" }
        return Money(amount + other.amount, currency)
    }

    operator fun times(multiplier: Int): Money {
        return Money(amount * multiplier.toBigDecimal(), currency)
    }

    companion object {
        val ZERO = Money(BigDecimal.ZERO)
    }
}
```

```kotlin
// domain/valueobjects/Address.kt
package features.ordermanagement.domain.valueobjects

data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
) {
    init {
        require(street.isNotBlank()) { "Street cannot be blank" }
        require(city.isNotBlank()) { "City cannot be blank" }
        require(zipCode.matches(Regex("\\d{5}"))) { "Invalid ZIP code: $zipCode" }
        require(country.isNotBlank()) { "Country cannot be blank" }
    }
}
```

```kotlin
// domain/valueobjects/OrderStatus.kt
package features.ordermanagement.domain.valueobjects

enum class OrderStatus {
    DRAFT,      // Order being created
    SUBMITTED,  // Order submitted, awaiting payment
    PAID,       // Payment confirmed
    SHIPPED,    // Order shipped
    DELIVERED,  // Order delivered
    CANCELLED   // Order cancelled
}
```

### 2. Domain Entities

```kotlin
// domain/entities/OrderItem.kt
package features.ordermanagement.domain.entities

import features.ordermanagement.domain.valueobjects.Money
import java.util.UUID

data class OrderItem(
    val id: UUID,
    val orderId: UUID,
    val productId: UUID,
    val productName: String,
    val quantity: Int,
    val price: Money
) {
    init {
        require(quantity > 0) { "Quantity must be positive" }
        require(productName.isNotBlank()) { "Product name cannot be blank" }
    }

    val subtotal: Money
        get() = price * quantity
}
```

### 3. Aggregate Root

```kotlin
// domain/aggregates/Order.kt
package features.ordermanagement.domain.aggregates

import features.ordermanagement.domain.entities.OrderItem
import features.ordermanagement.domain.valueobjects.*
import java.time.Instant
import java.util.UUID

/**
 * Order Aggregate Root
 * Enforces business rules and maintains consistency boundaries
 */
class Order(
    val id: UUID = UUID.randomUUID(),
    val customerId: UUID,
    var shippingAddress: Address,
    private val items: MutableList<OrderItem> = mutableListOf(),
    var status: OrderStatus = OrderStatus.DRAFT,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
) {

    // Calculated properties
    val total: Money
        get() = if (items.isEmpty()) {
            Money.ZERO
        } else {
            items.fold(Money.ZERO) { acc, item -> acc + item.subtotal }
        }

    val itemCount: Int
        get() = items.size

    // Business rule: Add item
    fun addItem(productId: UUID, productName: String, quantity: Int, price: Money) {
        require(status == OrderStatus.DRAFT) {
            "Cannot add items to ${status.name} order. Only DRAFT orders can be modified."
        }
        require(quantity > 0) { "Quantity must be positive" }
        require(productName.isNotBlank()) { "Product name cannot be blank" }

        items.add(OrderItem(
            id = UUID.randomUUID(),
            orderId = id,
            productId = productId,
            productName = productName,
            quantity = quantity,
            price = price
        ))
        updatedAt = Instant.now()
    }

    // Business rule: Remove item
    fun removeItem(itemId: UUID) {
        require(status == OrderStatus.DRAFT) {
            "Cannot remove items from ${status.name} order"
        }

        val removed = items.removeIf { it.id == itemId }
        if (!removed) {
            throw OrderItemNotFoundException(itemId)
        }

        updatedAt = Instant.now()
    }

    // Business rule: Submit order
    fun submit() {
        require(status == OrderStatus.DRAFT) {
            "Order already submitted (status: ${status.name})"
        }
        require(items.isNotEmpty()) {
            "Cannot submit empty order"
        }
        require(total.amount >= BigDecimal("10.00")) {
            "Minimum order amount is $10.00, current total: ${total.amount}"
        }

        status = OrderStatus.SUBMITTED
        updatedAt = Instant.now()
    }

    // Business rule: Mark as paid
    fun markAsPaid() {
        require(status == OrderStatus.SUBMITTED) {
            "Can only mark SUBMITTED orders as PAID. Current status: ${status.name}"
        }

        status = OrderStatus.PAID
        updatedAt = Instant.now()
    }

    // Business rule: Ship order
    fun ship() {
        require(status == OrderStatus.PAID) {
            "Can only ship PAID orders. Current status: ${status.name}"
        }

        status = OrderStatus.SHIPPED
        updatedAt = Instant.now()
    }

    // Business rule: Deliver order
    fun deliver() {
        require(status == OrderStatus.SHIPPED) {
            "Can only deliver SHIPPED orders. Current status: ${status.name}"
        }

        status = OrderStatus.DELIVERED
        updatedAt = Instant.now()
    }

    // Business rule: Cancel order
    fun cancel() {
        require(status in listOf(OrderStatus.DRAFT, OrderStatus.SUBMITTED)) {
            "Cannot cancel ${status.name} order. Only DRAFT or SUBMITTED orders can be cancelled."
        }

        status = OrderStatus.CANCELLED
        updatedAt = Instant.now()
    }

    // Business rule: Update shipping address
    fun updateShippingAddress(newAddress: Address) {
        require(status in listOf(OrderStatus.DRAFT, OrderStatus.SUBMITTED)) {
            "Cannot update address for ${status.name} order"
        }

        shippingAddress = newAddress
        updatedAt = Instant.now()
    }

    // Expose items as read-only
    fun getItems(): List<OrderItem> = items.toList()
}

// Domain exceptions
class OrderItemNotFoundException(itemId: UUID) : RuntimeException("Order item $itemId not found")
```

### 4. DTOs (API Contracts)

```kotlin
// dto/OrderRequests.kt
package features.ordermanagement.dto

import java.math.BigDecimal
import java.util.UUID
import javax.validation.constraints.*

data class CreateOrderRequest(
    @field:NotBlank
    val street: String,

    @field:NotBlank
    val city: String,

    @field:Pattern(regexp = "\\d{5}", message = "ZIP code must be 5 digits")
    val zipCode: String,

    @field:NotBlank
    val country: String
)

data class AddOrderItemRequest(
    @field:NotNull
    val productId: UUID,

    @field:NotBlank
    val productName: String,

    @field:Min(1, message = "Quantity must be at least 1")
    val quantity: Int,

    @field:DecimalMin("0.01", message = "Price must be positive")
    val price: BigDecimal,

    val currency: String = "USD"
)

data class UpdateAddressRequest(
    @field:NotBlank
    val street: String,

    @field:NotBlank
    val city: String,

    @field:Pattern(regexp = "\\d{5}")
    val zipCode: String,

    @field:NotBlank
    val country: String
)
```

```kotlin
// dto/OrderResponses.kt
package features.ordermanagement.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val customerId: UUID,
    val shippingAddress: AddressResponse,
    val items: List<OrderItemResponse>,
    val total: MoneyResponse,
    val status: String,
    val itemCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class OrderItemResponse(
    val id: UUID,
    val productId: UUID,
    val productName: String,
    val quantity: Int,
    val price: MoneyResponse,
    val subtotal: MoneyResponse
)

data class AddressResponse(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
)

data class MoneyResponse(
    val amount: BigDecimal,
    val currency: String
)
```

### 5. Controller

```kotlin
// controllers/OrderController.kt
package features.ordermanagement.controllers

import features.ordermanagement.dto.*
import features.ordermanagement.services.OrderService
import org.springframework.http.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/api/orders")
@Validated
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    fun createOrder(
        @AuthenticationPrincipal customerId: UUID,
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(customerId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }

    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: UUID): ResponseEntity<OrderResponse> {
        val order = orderService.getById(orderId)
        return ResponseEntity.ok(order)
    }

    @PostMapping("/{orderId}/items")
    fun addItem(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: AddOrderItemRequest
    ): ResponseEntity<OrderResponse> {
        val order = orderService.addItem(orderId, request)
        return ResponseEntity.ok(order)
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    fun removeItem(
        @PathVariable orderId: UUID,
        @PathVariable itemId: UUID
    ): ResponseEntity<OrderResponse> {
        val order = orderService.removeItem(orderId, itemId)
        return ResponseEntity.ok(order)
    }

    @PostMapping("/{orderId}/submit")
    fun submitOrder(@PathVariable orderId: UUID): ResponseEntity<OrderResponse> {
        val order = orderService.submitOrder(orderId)
        return ResponseEntity.ok(order)
    }

    @PostMapping("/{orderId}/pay")
    fun markAsPaid(@PathVariable orderId: UUID): ResponseEntity<OrderResponse> {
        val order = orderService.markAsPaid(orderId)
        return ResponseEntity.ok(order)
    }

    @PostMapping("/{orderId}/ship")
    fun shipOrder(@PathVariable orderId: UUID): ResponseEntity<OrderResponse> {
        val order = orderService.ship(orderId)
        return ResponseEntity.ok(order)
    }

    @PutMapping("/{orderId}/address")
    fun updateAddress(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: UpdateAddressRequest
    ): ResponseEntity<OrderResponse> {
        val order = orderService.updateAddress(orderId, request)
        return ResponseEntity.ok(order)
    }

    @DeleteMapping("/{orderId}")
    fun cancelOrder(@PathVariable orderId: UUID): ResponseEntity<Unit> {
        orderService.cancelOrder(orderId)
        return ResponseEntity.noContent().build()
    }
}
```

### 6. Service (Orchestration Only)

```kotlin
// services/OrderService.kt
package features.ordermanagement.services

import features.ordermanagement.domain.aggregates.Order
import features.ordermanagement.domain.valueobjects.*
import features.ordermanagement.dto.*
import features.ordermanagement.repositories.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    @Transactional
    fun createOrder(customerId: UUID, request: CreateOrderRequest): OrderResponse {
        val address = Address(
            street = request.street,
            city = request.city,
            zipCode = request.zipCode,
            country = request.country
        )

        val order = Order(
            customerId = customerId,
            shippingAddress = address
        )

        val savedOrder = orderRepository.save(order)
        return savedOrder.toResponse()
    }

    @Transactional(readOnly = true)
    fun getById(orderId: UUID): OrderResponse {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)
        return order.toResponse()
    }

    @Transactional
    fun addItem(orderId: UUID, request: AddOrderItemRequest): OrderResponse {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)

        // Business logic in aggregate
        order.addItem(
            productId = request.productId,
            productName = request.productName,
            quantity = request.quantity,
            price = Money(request.price, request.currency)
        )

        val savedOrder = orderRepository.save(order)
        return savedOrder.toResponse()
    }

    @Transactional
    fun removeItem(orderId: UUID, itemId: UUID): OrderResponse {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)

        order.removeItem(itemId)

        val savedOrder = orderRepository.save(order)
        return savedOrder.toResponse()
    }

    @Transactional
    fun submitOrder(orderId: UUID): OrderResponse {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)

        order.submit()

        val savedOrder = orderRepository.save(order)
        return savedOrder.toResponse()
    }

    @Transactional
    fun markAsPaid(orderId: UUID): OrderResponse {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)

        order.markAsPaid()

        val savedOrder = orderRepository.save(order)
        return savedOrder.toResponse()
    }

    @Transactional
    fun ship(orderId: UUID): OrderResponse {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)

        order.ship()

        val savedOrder = orderRepository.save(order)
        return savedOrder.toResponse()
    }

    @Transactional
    fun updateAddress(orderId: UUID, request: UpdateAddressRequest): OrderResponse {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)

        val newAddress = Address(
            street = request.street,
            city = request.city,
            zipCode = request.zipCode,
            country = request.country
        )

        order.updateShippingAddress(newAddress)

        val savedOrder = orderRepository.save(order)
        return savedOrder.toResponse()
    }

    @Transactional
    fun cancelOrder(orderId: UUID) {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)

        order.cancel()
        orderRepository.save(order)
    }
}

// Service-level exceptions
class OrderNotFoundException(orderId: UUID) : RuntimeException("Order $orderId not found")
```

### 7. Repository (Saves Whole Aggregate)

```kotlin
// repositories/OrderRepository.kt
package features.ordermanagement.repositories

import features.ordermanagement.domain.aggregates.Order
import features.ordermanagement.domain.entities.OrderItem
import features.ordermanagement.domain.valueobjects.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Repository
class OrderRepository {

    /**
     * Save entire aggregate (Order + OrderItems)
     * This is the key DDD pattern: repository manages the whole aggregate
     */
    fun save(order: Order): Order = transaction {
        // Upsert order
        Orders.replace {
            it[id] = order.id
            it[customerId] = order.customerId
            it[street] = order.shippingAddress.street
            it[city] = order.shippingAddress.city
            it[zipCode] = order.shippingAddress.zipCode
            it[country] = order.shippingAddress.country
            it[status] = order.status.name
            it[createdAt] = order.createdAt
            it[updatedAt] = order.updatedAt
        }

        // Delete existing items and re-insert (simplest approach for aggregate)
        OrderItems.deleteWhere { OrderItems.orderId eq order.id }

        // Insert all items
        order.getItems().forEach { item ->
            OrderItems.insert {
                it[id] = item.id
                it[orderId] = order.id
                it[productId] = item.productId
                it[productName] = item.productName
                it[quantity] = item.quantity
                it[price] = item.price.amount
                it[currency] = item.price.currency
            }
        }

        findById(order.id)!!
    }

    /**
     * Load entire aggregate (Order with all OrderItems)
     */
    fun findById(id: UUID): Order? = transaction {
        val orderRow = Orders.select { Orders.id eq id }.singleOrNull()
            ?: return@transaction null

        // Load all items for this order
        val items = OrderItems.select { OrderItems.orderId eq id }
            .map { row ->
                OrderItem(
                    id = row[OrderItems.id],
                    orderId = row[OrderItems.orderId],
                    productId = row[OrderItems.productId],
                    productName = row[OrderItems.productName],
                    quantity = row[OrderItems.quantity],
                    price = Money(row[OrderItems.price], row[OrderItems.currency])
                )
            }

        // Reconstruct aggregate
        val order = Order(
            id = orderRow[Orders.id],
            customerId = orderRow[Orders.customerId],
            shippingAddress = Address(
                street = orderRow[Orders.street],
                city = orderRow[Orders.city],
                zipCode = orderRow[Orders.zipCode],
                country = orderRow[Orders.country]
            ),
            status = OrderStatus.valueOf(orderRow[Orders.status]),
            createdAt = orderRow[Orders.createdAt],
            updatedAt = orderRow[Orders.updatedAt]
        )

        // Re-add items to aggregate (bypassing business rules for reconstruction)
        items.forEach { item ->
            // Use reflection or make addItem internal for repository
            order.addItem(
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                price = item.price
            )
        }

        order
    }

    fun findByCustomerId(customerId: UUID): List<Order> = transaction {
        Orders.select { Orders.customerId eq customerId }
            .map { findById(it[Orders.id])!! }
    }
}

// Table definitions
object Orders : Table("orders") {
    val id = uuid("id")
    val customerId = uuid("customer_id")
    val street = varchar("street", 255)
    val city = varchar("city", 100)
    val zipCode = varchar("zip_code", 10)
    val country = varchar("country", 100)
    val status = varchar("status", 50)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object OrderItems : Table("order_items") {
    val id = uuid("id")
    val orderId = uuid("order_id").references(Orders.id, onDelete = ReferenceOption.CASCADE)
    val productId = uuid("product_id")
    val productName = varchar("product_name", 255)
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2)
    val currency = varchar("currency", 3)

    override val primaryKey = PrimaryKey(id)
}
```

### 8. Mappers

```kotlin
// mappers/OrderMapper.kt (add to dto package or create mappers package)
package features.ordermanagement.mappers

import features.ordermanagement.domain.aggregates.Order
import features.ordermanagement.domain.entities.OrderItem
import features.ordermanagement.domain.valueobjects.*
import features.ordermanagement.dto.*

fun Order.toResponse() = OrderResponse(
    id = id,
    customerId = customerId,
    shippingAddress = shippingAddress.toResponse(),
    items = getItems().map { it.toResponse() },
    total = total.toResponse(),
    status = status.name,
    itemCount = itemCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun OrderItem.toResponse() = OrderItemResponse(
    id = id,
    productId = productId,
    productName = productName,
    quantity = quantity,
    price = price.toResponse(),
    subtotal = subtotal.toResponse()
)

fun Address.toResponse() = AddressResponse(
    street = street,
    city = city,
    zipCode = zipCode,
    country = country
)

fun Money.toResponse() = MoneyResponse(
    amount = amount,
    currency = currency
)
```

### 9. Database Migration

```sql
-- db/migration/V3__create_orders.sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    country VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD'
);

-- Indexes for performance
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

### 10. Tests

```kotlin
// domain/aggregates/OrderTest.kt
package features.ordermanagement.domain.aggregates

import features.ordermanagement.domain.valueobjects.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.util.UUID

class OrderTest {

    @Test
    fun `should create empty draft order`() {
        val address = Address("123 Main St", "New York", "10001", "USA")
        val customerId = UUID.randomUUID()

        val order = Order(
            customerId = customerId,
            shippingAddress = address
        )

        assertThat(order.status).isEqualTo(OrderStatus.DRAFT)
        assertThat(order.itemCount).isEqualTo(0)
        assertThat(order.total).isEqualTo(Money.ZERO)
    }

    @Test
    fun `should add item to draft order`() {
        val order = createDraftOrder()

        order.addItem(
            productId = UUID.randomUUID(),
            productName = "Product 1",
            quantity = 2,
            price = Money(BigDecimal("10.00"))
        )

        assertThat(order.itemCount).isEqualTo(1)
        assertThat(order.total.amount).isEqualByComparingTo(BigDecimal("20.00"))
    }

    @Test
    fun `should not add item to submitted order`() {
        val order = createDraftOrder()
        order.addItem(UUID.randomUUID(), "Product", 1, Money(BigDecimal("15.00")))
        order.submit()

        assertThrows<IllegalArgumentException> {
            order.addItem(UUID.randomUUID(), "Product 2", 1, Money(BigDecimal("10.00")))
        }
    }

    @Test
    fun `should submit order with minimum amount`() {
        val order = createDraftOrder()
        order.addItem(UUID.randomUUID(), "Product", 1, Money(BigDecimal("15.00")))

        order.submit()

        assertThat(order.status).isEqualTo(OrderStatus.SUBMITTED)
    }

    @Test
    fun `should not submit empty order`() {
        val order = createDraftOrder()

        assertThrows<IllegalArgumentException> {
            order.submit()
        }
    }

    @Test
    fun `should not submit order below minimum amount`() {
        val order = createDraftOrder()
        order.addItem(UUID.randomUUID(), "Product", 1, Money(BigDecimal("5.00")))

        assertThrows<IllegalArgumentException> {
            order.submit()
        }
    }

    @Test
    fun `should transition through valid status flow`() {
        val order = createDraftOrder()
        order.addItem(UUID.randomUUID(), "Product", 1, Money(BigDecimal("15.00")))

        // DRAFT -> SUBMITTED
        order.submit()
        assertThat(order.status).isEqualTo(OrderStatus.SUBMITTED)

        // SUBMITTED -> PAID
        order.markAsPaid()
        assertThat(order.status).isEqualTo(OrderStatus.PAID)

        // PAID -> SHIPPED
        order.ship()
        assertThat(order.status).isEqualTo(OrderStatus.SHIPPED)

        // SHIPPED -> DELIVERED
        order.deliver()
        assertThat(order.status).isEqualTo(OrderStatus.DELIVERED)
    }

    @Test
    fun `should cancel draft or submitted order`() {
        val order = createDraftOrder()
        order.addItem(UUID.randomUUID(), "Product", 1, Money(BigDecimal("15.00")))
        order.submit()

        order.cancel()

        assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    fun `should not cancel paid order`() {
        val order = createDraftOrder()
        order.addItem(UUID.randomUUID(), "Product", 1, Money(BigDecimal("15.00")))
        order.submit()
        order.markAsPaid()

        assertThrows<IllegalArgumentException> {
            order.cancel()
        }
    }

    private fun createDraftOrder(): Order {
        val address = Address("123 Main St", "New York", "10001", "USA")
        return Order(
            customerId = UUID.randomUUID(),
            shippingAddress = address
        )
    }
}
```

---

## Key Takeaways

### Simple Features (Example 1 - Authentication)
**Use when:**
- Basic CRUD operations
- Simple validation rules
- Single entity features
- No complex business logic
- Straightforward workflows

**Structure:**
- No `domain/` folder needed
- Entities in `models/` folder
- Business logic in services
- Direct repository usage

**Good for:**
- User registration/login
- User profiles
- Basic settings
- Simple content management

---

### Complex Features (Example 2 - Order Management)
**Use when:**
- Multiple related entities needing consistency
- Complex business invariants
- Rich behavior in models
- Transaction boundaries across entities
- Workflow with state transitions

**Structure:**
- Add `domain/` folder
- **Aggregates** enforce business rules
- **Value Objects** for immutable concepts
- **Entities** for objects with identity
- Repository saves whole aggregate

**Good for:**
- E-commerce orders
- Shopping carts
- Booking/reservation systems
- Financial transactions
- Workflow management

---

## Architecture Decision Tree

```
Is your feature...

├─ Simple CRUD with basic validation?
│  └─ → Use Simple Architecture (Example 1)
│
├─ Has multiple entities that must stay consistent?
│  └─ → Use DDD with Aggregates (Example 2)
│
├─ Has complex business rules spanning entities?
│  └─ → Use DDD with Aggregates (Example 2)
│
├─ Requires transaction boundaries?
│  └─ → Use DDD with Aggregates (Example 2)
│
└─ Has immutable concepts (Money, Address, Email)?
   └─ → Use Value Objects (can be in simple or DDD)
```

---

## Critical Patterns Summary

### ✅ Always Remember

1. **UUID for IDs**
   ```kotlin
   val id: UUID = UUID.randomUUID()
   ```

2. **Instant for Timestamps**
   ```kotlin
   val createdAt: Instant = Instant.now()
   ```

3. **Exposed ORM with UUID and Instant**
   ```kotlin
   object MyTable : Table("my_table") {
       val id = uuid("id")
       val createdAt = timestamp("created_at")
   }
   ```

4. **Flyway Migrations**
   ```sql
   CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
   CREATE TABLE my_table (
       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
   );
   ```

5. **Aggregates Enforce Business Rules**
   ```kotlin
   class Order {
       fun submit() {
           require(status == OrderStatus.DRAFT) { "Already submitted" }
           require(items.isNotEmpty()) { "Cannot submit empty order" }
           // Enforce invariants here
       }
   }
   ```

---

**Remember**: Start simple, add complexity only when needed. The authentication example shows you can build a complete feature without DDD patterns. The order management example shows when and how to add them.
