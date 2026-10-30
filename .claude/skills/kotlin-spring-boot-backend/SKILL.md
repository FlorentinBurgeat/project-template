---
name: kotlin-spring-boot-backend
description: Comprehensive backend development guide for Kotlin/Spring Boot microservices. Use when creating REST controllers, services, repositories, dependency injection, middleware, or working with Spring Boot APIs, Exposed ORM, JWT authentication, database migrations, or error handling. Covers layered architecture (routes → controllers → services → repositories), annotation patterns, configuration management, testing strategies, and Spring Boot best practices in Kotlin.
---

# Kotlin/Spring Boot Backend Development Guidelines

## Purpose

Establish consistency and best practices across your Kotlin/Spring Boot backend using modern Spring Boot patterns adapted for Kotlin idioms.

## When to Use This Skill

Automatically activates when working on:
- Creating or modifying REST controllers, endpoints, APIs
- Building services and business logic
- Implementing repositories and data access
- Setting up dependency injection and Spring beans
- Implementing middleware and filters
- Database operations with Exposed ORM
- Input validation and error handling
- JWT authentication and security configuration
- Backend testing and refactoring

---

## Quick Start

### New Bounded Context Checklist

Convention: `.claude/rules/back-architecture.md` and `docs/adr/0003-modular-monolith-hexagonal.md`.

- [ ] **Module**: top-level package `com.template.<name>`
- [ ] **Domain**: `domain/<aggregate>/model` (data only, value classes), `logic` (pure functions), `port` (interfaces)
- [ ] **Application**: `@Service` use cases depending on the domain only, returning sealed results
- [ ] **Web adapter**: controller, request/response DTOs, mapper, scoped exception handler
- [ ] **Persistence adapter**: Exposed tables, records, record mapper, repository implementing the port
- [ ] **Migration**: Flyway SQL script if the schema changes
- [ ] **Tests**: pure unit tests for `logic/`, API tests (Testcontainers), `ModularityTest`, `HexagonalArchitectureTest`
- [ ] **API contract**: `@SecurityRequirement(name = BEARER_AUTH)` on protected controllers, refresh `openapi/template-api.json` (ADR 0004)

### Package Structure

```
com/template/<name>/
├── domain/<aggregate>/
│   ├── model/          # data class, value class, enum, sealed (no logic)
│   ├── logic/          # pure functions: rules, calculations
│   └── port/           # ItemRepository (interface)
├── application/        # ItemService (@Service), sealed results
└── adapters/
    ├── web/            # Controller, ExceptionHandler, Request/Response, WebMapper
    └── persistence/    # Tables, Records, RecordMapper, ExposedXxxRepository
```

Technical modules without a domain: `shared/` (ErrorResponse, DatabaseConfig, JacksonConfig, OpenApiConfig, UserId) and `auth/`.

---

## Architecture Overview

**Modular monolith (Spring Modulith), hexagonal per bounded context.** Dependencies point inward: adapters -> application -> domain. The domain has no Spring, Exposed or Jackson. Aggregates are anemic: `model/` holds data, `logic/` holds pure functions. Use `@JvmInline value class` instead of primitives and `typealias`. Domain words in the project's business language, technical roles in English.

```
HTTP Request
    ↓
Controller (adapters/web: required fields, DTO -> domain)
    ↓
Service (application: use case, sealed result)
    ↓
Domain logic (pure functions on the model)
    ↓
Port ← ExposedRepository (adapters/persistence: records <-> domain)
```

> `examples.md` predates this convention (feature-based layout). Follow the rule above where they differ.

---

## Directory Structure

```
back/src/
├── main/kotlin/com/template/
│   ├── Application.kt
│   ├── shared/            # ErrorResponse, DatabaseConfig, JacksonConfig, OpenApiConfig, UserId
│   ├── auth/              # config/, keycloak/, web/
│   └── example/           # bounded context (see Package Structure)
├── main/resources/db/migration/   # Flyway
└── test/kotlin/com/template/          # same tree, plus ModularityTest and HexagonalArchitectureTest
```

**Naming Conventions:** `PascalCase` files. Controllers `XxxController`, use cases `XxxService`, ports `XxxRepository`, implementations `ExposedXxxRepository`, DTOs `XxxRequest` / `XxxResponse`, Exposed tables `Xxxs`, persistence shapes `XxxRecord`.

---

## Key Patterns

### 1. Controllers - Request Handling

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    @PostMapping
    fun createUser(@Valid @RequestBody dto: CreateUserDto): ResponseEntity<UserResponse> {
        val user = userService.createUser(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(user.toResponse())
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): ResponseEntity<UserResponse> {
        val user = userService.getUserById(id)
        return ResponseEntity.ok(user.toResponse())
    }
}
```

**Rules:**
- Extend with `@RestController` annotation
- Use constructor injection for dependencies
- Keep methods focused on HTTP handling
- Delegate business logic to services
- Return `ResponseEntity` for consistent response handling
- Validate input with `@Valid` annotations

See [reference.md](reference.md) for complete controller patterns.

### 2. Services - Business Logic

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    fun createUser(dto: CreateUserDto): User {
        validateEmail(dto.email)
        val user = userRepository.save(dto.toEntity())
        emailService.sendWelcomeEmail(user.email)
        return user
    }

    fun getUserById(id: UUID): User {
        return userRepository.findById(id)
            ?: throw UserNotFoundException("User $id not found")
    }
}
```

**Rules:**
- Use `@Service` annotation
- Constructor injection for dependencies
- Business logic lives here, not in controllers
- Throw meaningful exceptions for error cases
- Use repository for data access

See [reference.md](reference.md) for complete service patterns.

### 3. Repositories - Data Access with Exposed ORM

```kotlin
import java.util.UUID

@Repository
class UserRepository {
    fun save(user: User): User {
        return transaction {
            Users.insert {
                it[id] = user.id
                it[name] = user.name
                it[email] = user.email
                it[passwordHash] = user.passwordHash
            }
            findById(user.id)!!
        }
    }

    fun findById(id: UUID): User? {
        return transaction {
            Users.select { Users.id eq id }
                .map { it.toUser() }
                .firstOrNull()
        }
    }
}
```

**Rules:**
- Use `@Repository` annotation
- Wrap database operations in `transaction { }`
- Use Exposed's DSL for queries
- Return domain objects, not database records
- Keep queries focused and efficient

See [reference.md](reference.md) for Exposed ORM patterns.

### 4. Dependency Injection - Spring Configuration

```kotlin
@Configuration
class SecurityConfig(
    private val jwtProvider: JwtProvider
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeRequests { authz ->
                authz
                    .antMatchers("/auth/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
```

**Rules:**
- Use Spring's constructor injection everywhere
- Define beans in `@Configuration` classes
- Keep configuration classes focused
- Use Kotlin's `private val` for immutability

See [reference.md](reference.md) for DI patterns.

### 5. Validation & Error Handling

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .associate { it.field to it.defaultMessage }
        return ResponseEntity.badRequest()
            .body(ErrorResponse("Validation failed", errors))
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleNotFound(ex: UserNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(ex.message ?: "Not found"))
    }
}

// Custom exception
class UserNotFoundException(message: String) : RuntimeException(message)
```

**Rules:**
- Use `@RestControllerAdvice` for global exception handling
- Define custom exceptions for domain-specific errors
- Return structured error responses
- Validate at the boundary (controller layer)

See [reference.md](reference.md) for validation patterns.

### 6. JWT Authentication

```kotlin
import java.util.UUID

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {
    fun generateToken(userId: UUID): String {
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact()
    }

    fun getUserIdFromToken(token: String): UUID? {
        return try {
            val claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .body
            UUID.fromString(claims.subject)
        } catch (e: Exception) {
            null
        }
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token)
            true
        } catch (e: JwtException) {
            false
        }
    }
}

@Component
class JwtFilter(private val jwtProvider: JwtProvider) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val token = extractToken(request)
        if (token != null && jwtProvider.validateToken(token)) {
            val userId = jwtProvider.getUserIdFromToken(token)
            // Set authentication context
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userId, null, emptyList())
        }
        chain.doFilter(request, response)
    }
}
```

See [reference.md](reference.md) for complete JWT patterns.

### 7. DDD Tactical Patterns (Optional - Use When Needed)

#### Aggregates - Transaction Boundaries

Use Aggregates when you have **multiple related entities that must stay consistent**:

```kotlin
// Order aggregate root - enforces business rules
class Order(
    val id: UUID,
    private val items: MutableList<OrderItem> = mutableListOf(),
    var status: OrderStatus = OrderStatus.DRAFT
) {
    // Business logic encapsulated in aggregate
    fun addItem(product: Product, quantity: Int) {
        if (status != OrderStatus.DRAFT) {
            throw IllegalStateException("Cannot modify non-draft order")
        }
        items.add(OrderItem(UUID.randomUUID(), product.id, quantity, product.price))
    }

    fun calculateTotal(): Money {
        return Money(items.sumOf { it.price.amount * it.quantity })
    }

    fun submit() {
        if (items.isEmpty()) {
            throw IllegalStateException("Cannot submit empty order")
        }
        status = OrderStatus.SUBMITTED
    }
}

// Entity within the aggregate
data class OrderItem(
    val id: UUID,
    val productId: UUID,
    val quantity: Int,
    val price: Money
)
```

**When to use:**
- Multiple entities needing consistency (Order + OrderItems)
- Complex business invariants across entities
- Transaction boundaries (save aggregate as a whole)

#### Value Objects - Immutable Values

Use Value Objects for **concepts defined by their attributes, not identity**:

```kotlin
// Money value object - immutable, no identity
data class Money(
    val amount: BigDecimal,
    val currency: String = "USD"
) {
    init {
        require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return Money(amount + other.amount, currency)
    }
}

// Email value object - with validation
data class Email(val value: String) {
    init {
        require(value.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
            "Invalid email format"
        }
    }
}

// Address value object
data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
)
```

**When to use:**
- Immutable values (Money, Email, Address)
- No identity needed (two $10 bills are the same)
- Encapsulate validation logic
- Make domain concepts explicit

#### Simple Entities - When Domain Layer Not Needed

For simple features, keep it straightforward:

```kotlin
// Simple entity in models/ folder (no domain folder needed)
data class User(
    val id: UUID = UUID.randomUUID(),
    val email: String,
    val name: String,
    val passwordHash: String,
    val createdAt: Instant = Instant.now()
)
```

**Decision Guide:**
- **Simple Entity**: CRUD operations, basic validation → Keep in `models/`
- **Rich Entity**: Complex behavior, business rules → Move to `domain/entities/`
- **Aggregate**: Multiple entities, consistency needed → Use `domain/aggregates/`
- **Value Object**: Immutable, no identity → Use `domain/valueobjects/`

---

## Database Patterns

### Exposed ORM Basics

**CRITICAL RULES:**
- Always use UUID for primary keys, never Long/BIGSERIAL
- Always use Instant (UTC timestamps), never LocalDateTime
- Use `timestamp()` in Exposed, which maps to Instant

```kotlin
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

// Define table with UUID primary key and UTC timestamps
object Users : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// Domain model with UUID and Instant
data class User(
    val id: UUID = UUID.randomUUID(),
    val email: String,
    val name: String,
    val passwordHash: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

// Conversion function
fun ResultRow.toUser() = User(
    id = this[Users.id],
    email = this[Users.email],
    name = this[Users.name],
    passwordHash = this[Users.passwordHash],
    createdAt = this[Users.createdAt],
    updatedAt = this[Users.updatedAt]
)
```

### Flyway Migrations

**CRITICAL RULES:**
- Always enable UUID extension
- Always use UUID for primary keys
- Always use TIMESTAMPTZ for timestamps (stores in UTC)

```sql
-- db/migration/V1__init_users_table.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

See [reference.md](reference.md) for complete database patterns.

---

## Testing

### Unit Test Example

```kotlin
@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var userService: UserService

    @Test
    fun `should create user successfully`() {
        val dto = CreateUserDto("john@example.com", "John")
        val userId = UUID.randomUUID()
        val now = Instant.now()
        val user = User(userId, dto.email, dto.name, "hashed", now, now)

        whenever(userRepository.save(any())).thenReturn(user)

        val result = userService.createUser(dto)

        assertThat(result.email).isEqualTo(dto.email)
        verify(userRepository).save(any())
    }
}
```

See [reference.md](reference.md) for testing patterns.

---

## Configuration

### application.yml

```yaml
spring:
  application:
    name: backend
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:appdb}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:password}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    locations: classpath:db/migration

server:
  port: 8080

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 hours
```

---

## Best Practices Summary

✅ **DO:**
- **ALWAYS use UUID for primary keys** - Never use Long/BIGSERIAL for IDs
- **ALWAYS use UTC timestamps (Instant)** - Never use LocalDateTime for timestamps
- Use constructor injection (immutable dependencies)
- Keep layers separated (Controller → Service → Repository)
- Throw meaningful exceptions
- Use `ResponseEntity` for consistent responses
- Validate input at boundaries
- Write tests alongside code
- Use Kotlin idioms (`val`, data classes, extension functions)

❌ **DON'T:**
- **Use Long or BIGSERIAL for primary keys** - Always use UUID instead
- **Use LocalDateTime for timestamps** - Always use Instant (UTC) instead
- Put business logic in controllers
- Use field injection with `@Autowired`
- Return raw database objects from services
- Ignore error handling
- Mix Kotlin and Java patterns inconsistently
- Add logic to repositories
- Hardcode configuration values

---

## Further Reading

- See [reference.md](reference.md) for detailed patterns and code examples
- See [examples.md](examples.md) for real-world implementations
- Check `CLAUDE.md` in project root for project-wide conventions

---

**Last Updated:** December 2024
