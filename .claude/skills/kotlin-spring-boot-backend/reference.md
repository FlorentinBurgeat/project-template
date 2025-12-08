# Kotlin/Spring Boot Backend - Reference Guide

## Controllers

### Basic REST Controller

```kotlin
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
    fun getById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.getById(id)
        return ResponseEntity.ok(user.toResponse())
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody dto: UpdateUserDto
    ): ResponseEntity<UserResponse> {
        val user = userService.update(id, dto)
        return ResponseEntity.ok(user.toResponse())
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
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
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val createdAt: LocalDateTime
)

data class ErrorResponse(
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val details: Map<String, String>? = null
)
```

### Conversion Extension Functions

```kotlin
fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    name = name,
    createdAt = createdAt
)

fun CreateUserDto.toEntity() = User(
    id = 0L,  // Database will assign
    email = email,
    name = name,
    passwordHash = hashPassword(password),
    createdAt = LocalDateTime.now()
)
```

---

## Services

### Basic Service Pattern

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder
) {

    fun create(dto: CreateUserDto): User {
        validateEmailUnique(dto.email)

        val user = User(
            id = 0L,
            email = dto.email,
            name = dto.name,
            passwordHash = passwordEncoder.encode(dto.password),
            createdAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(user)
        emailService.sendWelcomeEmail(savedUser.email)

        return savedUser
    }

    fun getById(id: Long): User {
        return userRepository.findById(id)
            ?: throw UserNotFoundException("User with ID $id not found")
    }

    fun update(id: Long, dto: UpdateUserDto): User {
        val user = getById(id)

        val updated = user.copy(
            email = dto.email ?: user.email,
            name = dto.name ?: user.name
        )

        return userRepository.save(updated)
    }

    fun delete(id: Long) {
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
@Service
class TransferService(
    private val accountRepository: AccountRepository
) {
    @Transactional
    fun transferMoney(fromId: Long, toId: Long, amount: BigDecimal) {
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
@Service
class NotificationService(
    private val emailService: EmailService,
    private val smsService: SmsService
) {
    @Async
    fun sendNotifications(userId: Long) {
        val user = userRepository.findById(userId) ?: return

        emailService.send(user.email, "Welcome!")
        smsService.send(user.phone, "Welcome to our app!")
    }
}
```

---

## Repositories with Exposed ORM

### Table Definition

```kotlin
object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}

object UserRoles : Table("user_roles") {
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 50)

    override val primaryKey = PrimaryKey(userId, role)
}
```

### Repository Implementation

```kotlin
@Repository
class UserRepository {

    fun save(user: User): User = transaction {
        val id = if (user.id == 0L) {
            Users.insert {
                it[email] = user.email
                it[name] = user.name
                it[passwordHash] = user.passwordHash
                it[createdAt] = user.createdAt
            } get Users.id
        } else {
            Users.update({ Users.id eq user.id }) {
                it[name] = user.name
                it[email] = user.email
                it[updatedAt] = LocalDateTime.now()
            }
            user.id
        }
        findById(id)!!
    }

    fun findById(id: Long): User? = transaction {
        Users.select { Users.id eq id }
            .map { it.toUser() }
            .firstOrNull()
    }

    fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .map { it.toUser() }
            .firstOrNull()
    }

    fun findAll(): List<User> = transaction {
        Users.selectAll()
            .map { it.toUser() }
    }

    fun deleteById(id: Long): Boolean = transaction {
        Users.deleteWhere { Users.id eq id } > 0
    }

    fun existsById(id: Long): Boolean = transaction {
        Users.select { Users.id eq id }.count() > 0
    }
}
```

### Conversion Function

```kotlin
private fun ResultRow.toUser() = User(
    id = this[Users.id],
    email = this[Users.email],
    name = this[Users.name],
    passwordHash = this[Users.passwordHash],
    createdAt = this[Users.createdAt]
)
```

### Complex Queries

```kotlin
// Join query
fun findUserWithRoles(id: Long): UserWithRoles? = transaction {
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
```

---

## Exception Handling

### Custom Exceptions

```kotlin
sealed class AppException(message: String) : RuntimeException(message)

class UserNotFoundException(id: Long) :
    AppException("User with ID $id not found")

class EmailAlreadyInUseException(email: String) :
    AppException("Email $email is already in use")

class InvalidCredentialsException :
    AppException("Invalid email or password")

class UnauthorizedException(message: String = "Unauthorized") :
    AppException(message)

class InsufficientFundsException(message: String) :
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
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val details: Map<String, String>? = null
)
```

---

## Security & JWT

### JWT Provider

```kotlin
@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {

    fun generateToken(userId: Long): String {
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

    fun getUserIdFromToken(token: String): Long {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .body
            .subject
            .toLong()
    }
}
```

### JWT Filter

```kotlin
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
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userId, null, emptyList())
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
@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var emailService: EmailService

    @InjectMocks
    private lateinit var userService: UserService

    @Test
    fun `should create user successfully`() {
        // Arrange
        val dto = CreateUserDto("john@example.com", "John", "password123")
        val savedUser = User(1L, "john@example.com", "John", "hashed", LocalDateTime.now())

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
        whenever(userRepository.findByEmail("john@example.com"))
            .thenReturn(User(1L, "john@example.com", "John", "hashed", LocalDateTime.now()))

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

### V1__init.sql

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);
```

---

**Remember:** Keep your code organized, testable, and maintainable. Follow these patterns consistently across your backend.
