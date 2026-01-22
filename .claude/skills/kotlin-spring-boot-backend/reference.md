# Kotlin/Spring Boot Backend - Reference Guide

**CRITICAL RULES:**
- **ALWAYS use UUID for primary keys** - Never Long/BIGSERIAL
- **ALWAYS use Instant (UTC) for timestamps** - Never LocalDateTime
- **Use `timestamp()` in Exposed** - Maps to `TIMESTAMPTZ` in PostgreSQL

---

## Controllers

### Basic REST Controller

```kotlin
import java.util.UUID

@RestController
@RequestMapping("/api/users")
@Validated
class UserController(
    private val userService: UserService
) {
    @PostMapping
    fun create(@Valid @RequestBody dto: CreateUserDto): ResponseEntity<UserResponse> {
        val user = userService.create(dto)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(user.toResponse())
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<UserResponse> {
        val user = userService.getById(id)
        return ResponseEntity.ok(user.toResponse())
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateUserDto
    ): ResponseEntity<UserResponse> {
        val user = userService.update(id, dto)
        return ResponseEntity.ok(user.toResponse())
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Unit> {
        userService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
```

### Request DTOs with Validation

```kotlin
data class CreateUserDto(
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

data class UpdateUserDto(
    @field:Email(message = "Email must be valid")
    val email: String? = null,

    @field:Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    val name: String? = null
)
```

### Response DTOs

```kotlin
import java.time.Instant
import java.util.UUID

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

### Conversion Extension Functions

```kotlin
import java.time.Instant
import java.util.UUID

fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    name = name,
    createdAt = createdAt
)

fun CreateUserDto.toEntity() = User(
    id = UUID.randomUUID(),
    email = email,
    name = name,
    passwordHash = hashPassword(password),
    createdAt = Instant.now()
)
```

---

## Services

### Basic Service Pattern

```kotlin
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder
) {

    fun create(dto: CreateUserDto): User {
        validateEmailUnique(dto.email)

        val user = User(
            id = UUID.randomUUID(),
            email = dto.email,
            name = dto.name,
            passwordHash = passwordEncoder.encode(dto.password),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val savedUser = userRepository.save(user)
        emailService.sendWelcomeEmail(savedUser.email)

        return savedUser
    }

    fun getById(id: UUID): User {
        return userRepository.findById(id)
            ?: throw UserNotFoundException("User with ID $id not found")
    }

    fun update(id: UUID, dto: UpdateUserDto): User {
        val user = getById(id)

        val updated = user.copy(
            email = dto.email ?: user.email,
            name = dto.name ?: user.name,
            updatedAt = Instant.now()
        )

        return userRepository.save(updated)
    }

    fun delete(id: UUID) {
        userRepository.deleteById(id)
    }

    private fun validateEmailUnique(email: String) {
        if (userRepository.findByEmail(email) != null) {
            throw EmailAlreadyInUseException("Email $email is already registered")
        }
    }
}
```

### Transaction Management

```kotlin
import java.math.BigDecimal
import java.util.UUID

@Service
class TransferService(
    private val accountRepository: AccountRepository
) {
    @Transactional
    fun transferMoney(fromId: UUID, toId: UUID, amount: BigDecimal) {
        val from = accountRepository.findById(fromId)
            ?: throw AccountNotFoundException("From account not found")
        val to = accountRepository.findById(toId)
            ?: throw AccountNotFoundException("To account not found")

        if (from.balance < amount) {
            throw InsufficientFundsException("Insufficient funds")
        }

        from.balance -= amount
        to.balance += amount

        accountRepository.save(from)
        accountRepository.save(to)
    }
}
```

### Async Operations

```kotlin
import java.util.UUID

@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val smsService: SmsService
) {
    @Async
    fun sendNotifications(userId: UUID) {
        val user = userRepository.findById(userId) ?: return

        emailService.send(user.email, "Welcome!")
        smsService.send(user.phone, "Welcome to our app!")
    }
}
```

---

## Repositories with Exposed ORM

**CRITICAL RULES FOR EXPOSED:**
- Use `uuid("column_name")` for UUID columns
- Use `timestamp("column_name")` for Instant (UTC timestamps)
- NEVER use `long()` for IDs or `datetime()` for timestamps

### Table Definition

```kotlin
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}

object UserRoles : Table("user_roles") {
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 50)

    override val primaryKey = PrimaryKey(userId, role)
}
```

### Repository Implementation

```kotlin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
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

    fun findAll(): List<User> = transaction {
        Users.selectAll()
            .map { it.toUser() }
    }

    fun deleteById(id: UUID): Boolean = transaction {
        Users.deleteWhere { Users.id eq id } > 0
    }

    fun existsById(id: UUID): Boolean = transaction {
        Users.select { Users.id eq id }.count() > 0
    }
}
```

### Conversion Function

```kotlin
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.util.UUID

private fun ResultRow.toUser() = User(
    id = this[Users.id],
    email = this[Users.email],
    name = this[Users.name],
    passwordHash = this[Users.passwordHash],
    createdAt = this[Users.createdAt],
    updatedAt = this[Users.updatedAt]
)
```

### Complex Queries

```kotlin
import java.util.UUID

// Join query
fun findUserWithRoles(id: UUID): UserWithRoles? = transaction {
    (Users innerJoin UserRoles)
        .select { Users.id eq id }
        .groupBy { it[Users.id] }
        .map { (_, rows) ->
            UserWithRoles(
                user = rows.first().toUser(),
                roles = rows.map { it[UserRoles.role] }
            )
        }
        .firstOrNull()
}

// Pagination
fun findAllPaginated(page: Int, pageSize: Int): Page<User> = transaction {
    val total = Users.selectAll().count()
    val users = Users.selectAll()
        .limit(pageSize, offset = (page * pageSize).toLong())
        .map { it.toUser() }

    Page(users, page, pageSize, total)
}

// Custom where clause
fun findActive(): List<User> = transaction {
    Users.select { Users.isActive eq true }
        .map { it.toUser() }
}

// Search by criteria
fun searchUsers(criteria: UserSearchCriteria): List<User> = transaction {
    Users.select {
        (Users.name like "%${criteria.namePattern}%") and
        (Users.isActive eq true)
    }
    .orderBy(Users.createdAt to SortOrder.DESC)
    .limit(criteria.limit)
    .map { it.toUser() }
}
```

---

## Exception Handling

### Custom Exceptions

```kotlin
import java.util.UUID

sealed class AppException(message: String) : RuntimeException(message)

class UserNotFoundException(id: UUID) :
    AppException("User with ID $id not found")

class EmailAlreadyInUseException(email: String) :
    AppException("Email $email is already in use")

class InvalidCredentialsException :
    AppException("Invalid email or password")

class UnauthorizedException(message: String = "Unauthorized") :
    AppException(message)

class InsufficientFundsException(message: String) :
    AppException(message)

class AccountNotFoundException(message: String) :
    AppException(message)
```

### Global Exception Handler

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(ex.message ?: "Not found"))
    }

    @ExceptionHandler(EmailAlreadyInUseException::class)
    fun handleEmailInUse(ex: EmailAlreadyInUseException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(ex.message ?: "Conflict"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "Invalid") }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                message = "Validation failed",
                details = errors
            ))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("An unexpected error occurred"))
    }
}

data class ErrorResponse(
    val message: String,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, String>? = null
)
```

---

## Security & JWT

### JWT Provider

```kotlin
import io.jsonwebtoken.*
import java.util.Date
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

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token)
            true
        } catch (e: JwtException) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID? {
        return try {
            val subject = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .body
                .subject
            UUID.fromString(subject)
        } catch (e: Exception) {
            null
        }
    }
}
```

### JWT Filter

```kotlin
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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
            if (userId != null) {
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(userId, null, emptyList())
            }
        }

        chain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        return if (header?.startsWith("Bearer ") == true) {
            header.substring(7)
        } else {
            null
        }
    }
}
```

### Security Configuration

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeRequests { authz ->
                authz
                    .antMatchers("/auth/register", "/auth/login").permitAll()
                    .antMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
```

---

## Testing

### Unit Test

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var userService: UserService

    @Test
    fun `should create user successfully`() {
        // Arrange
        val dto = CreateUserDto("john@example.com", "John", "password123")
        val userId = UUID.randomUUID()
        val now = Instant.now()
        val savedUser = User(userId, "john@example.com", "John", "hashed", now, now)

        whenever(passwordEncoder.encode(any())).thenReturn("hashed")
        whenever(userRepository.save(any())).thenReturn(savedUser)

        // Act
        val result = userService.create(dto)

        // Assert
        assertThat(result.email).isEqualTo("john@example.com")
        verify(userRepository).save(any())
        verify(emailService).sendWelcomeEmail("john@example.com")
    }

    @Test
    fun `should throw when email already exists`() {
        // Arrange
        val dto = CreateUserDto("john@example.com", "John", "password123")
        val existingUser = User(
            UUID.randomUUID(),
            "john@example.com",
            "John",
            "hashed",
            Instant.now(),
            Instant.now()
        )
        whenever(userRepository.findByEmail("john@example.com"))
            .thenReturn(existingUser)

        // Act & Assert
        assertThrows<EmailAlreadyInUseException> {
            userService.create(dto)
        }
    }
}
```

### Integration Test

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()
    }

    @Test
    fun `should create user endpoint`() {
        val dto = CreateUserDto("john@example.com", "John", "password123")

        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("john@example.com"))
    }
}
```

---

## Configuration Files

### application.yml

```yaml
spring:
  application:
    name: backend

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:appdb}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:password}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect

  flyway:
    locations: classpath:db/migration
    baseline-on-migrate: true

  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

server:
  port: 8080
  servlet:
    context-path: /api

jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  expiration: 86400000  # 24 hours

logging:
  level:
    root: INFO
    com.yourapp: DEBUG
```

### pom.xml (Key Dependencies)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
    <groupId>org.jetbrains.exposed</groupId>
    <artifactId>exposed-core</artifactId>
    <version>0.41.1</version>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.11.5</version>
</dependency>
```

---

## Database Migrations (Flyway)

**CRITICAL RULES FOR MIGRATIONS:**
- **ALWAYS enable UUID extension first**
- **ALWAYS use UUID for primary keys**
- **ALWAYS use TIMESTAMPTZ for timestamps** (stores in UTC)

### V1__init.sql

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table with UUID and TIMESTAMPTZ
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

-- User roles junction table
CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);
```

### V2__add_refresh_tokens.sql

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

---

## DDD Tactical Patterns (Optional)

Use these patterns when complexity warrants it. See main SKILL.md for when to use each.

### Aggregate Example

```kotlin
import java.time.Instant
import java.util.UUID

// Aggregate Root - enforces business rules
class Order(
    val id: UUID = UUID.randomUUID(),
    private val items: MutableList<OrderItem> = mutableListOf(),
    var status: OrderStatus = OrderStatus.DRAFT,
    val createdAt: Instant = Instant.now()
) {
    val total: Money
        get() = Money(items.sumOf { it.price.amount * it.quantity })

    fun addItem(productId: UUID, quantity: Int, price: Money) {
        if (status != OrderStatus.DRAFT) {
            throw IllegalStateException("Cannot modify submitted order")
        }
        items.add(OrderItem(UUID.randomUUID(), productId, quantity, price))
    }

    fun submit() {
        require(items.isNotEmpty()) { "Cannot submit empty order" }
        status = OrderStatus.SUBMITTED
    }

    fun getItems(): List<OrderItem> = items.toList()
}

// Entity within aggregate
data class OrderItem(
    val id: UUID,
    val productId: UUID,
    val quantity: Int,
    val price: Money
)

enum class OrderStatus {
    DRAFT, SUBMITTED, PAID, SHIPPED, DELIVERED
}
```

### Value Object Examples

```kotlin
import java.math.BigDecimal

// Money value object
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

    operator fun times(multiplier: Int): Money {
        return Money(amount * multiplier.toBigDecimal(), currency)
    }
}

// Email value object
data class Email(val value: String) {
    init {
        require(value.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
            "Invalid email format: $value"
        }
    }
}

// Address value object
data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
) {
    init {
        require(street.isNotBlank()) { "Street cannot be blank" }
        require(city.isNotBlank()) { "City cannot be blank" }
    }
}
```

### Repository for Aggregate

```kotlin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Repository
class OrderRepository {

    // Save entire aggregate
    fun save(order: Order): Order = transaction {
        // Insert order
        Orders.insert {
            it[id] = order.id
            it[status] = order.status.name
            it[createdAt] = order.createdAt
        }

        // Insert all order items
        order.getItems().forEach { item ->
            OrderItems.insert {
                it[id] = item.id
                it[orderId] = order.id
                it[productId] = item.productId
                it[quantity] = item.quantity
                it[price] = item.price.amount
            }
        }

        findById(order.id)!!
    }

    // Load entire aggregate
    fun findById(id: UUID): Order? = transaction {
        val orderRow = Orders.select { Orders.id eq id }.singleOrNull() ?: return@transaction null

        val items = OrderItems.select { OrderItems.orderId eq id }
            .map { row ->
                OrderItem(
                    id = row[OrderItems.id],
                    productId = row[OrderItems.productId],
                    quantity = row[OrderItems.quantity],
                    price = Money(row[OrderItems.price])
                )
            }

        Order(
            id = orderRow[Orders.id],
            status = OrderStatus.valueOf(orderRow[Orders.status]),
            createdAt = orderRow[Orders.createdAt]
        ).apply {
            items.forEach { addItem(it.productId, it.quantity, it.price) }
        }
    }
}
```

---

**Remember:**
- **Always use UUID for IDs** ✅
- **Always use Instant (UTC) for timestamps** ✅
- Keep your code organized, testable, and maintainable
- Follow these patterns consistently across your backend
- Add domain layer only when complexity warrants it
