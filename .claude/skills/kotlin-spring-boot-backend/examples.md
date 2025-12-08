# Real-World Examples - Kotlin/Spring Boot Backend

## Example 1: Complete User Feature (Registration)

### 1. Controller

```kotlin
@RestController
@RequestMapping("/api/auth")
@Validated
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody dto: RegisterDto): ResponseEntity<AuthResponse> {
        val auth = authService.register(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(auth)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody dto: LoginDto): ResponseEntity<AuthResponse> {
        val auth = authService.login(dto)
        return ResponseEntity.ok(auth)
    }
}
```

### 2. DTOs

```kotlin
data class RegisterDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 100)
    val name: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
)

data class LoginDto(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String
)
```

### 3. Service

```kotlin
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val emailService: EmailService
) {

    fun register(dto: RegisterDto): AuthResponse {
        validateEmailNotExists(dto.email)

        val user = User(
            id = 0L,
            email = dto.email,
            name = dto.name,
            passwordHash = passwordEncoder.encode(dto.password),
            createdAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(user)

        // Send welcome email async
        emailService.sendWelcomeEmail(savedUser.email, savedUser.name)

        val token = jwtProvider.generateToken(savedUser.id)

        return AuthResponse(
            token = token,
            user = UserResponse(
                id = savedUser.id,
                email = savedUser.email,
                name = savedUser.name
            )
        )
    }

    fun login(dto: LoginDto): AuthResponse {
        val user = userRepository.findByEmail(dto.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(dto.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val token = jwtProvider.generateToken(user.id)

        return AuthResponse(
            token = token,
            user = UserResponse(
                id = user.id,
                email = user.email,
                name = user.name
            )
        )
    }

    private fun validateEmailNotExists(email: String) {
        if (userRepository.findByEmail(email) != null) {
            throw EmailAlreadyInUseException("Email already registered")
        }
    }
}
```

### 4. Repository

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

    fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .map { it.toUser() }
            .firstOrNull()
    }

    fun findById(id: Long): User? = transaction {
        Users.select { Users.id eq id }
            .map { it.toUser() }
            .firstOrNull()
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        email = this[Users.email],
        name = this[Users.name],
        passwordHash = this[Users.passwordHash],
        createdAt = this[Users.createdAt]
    )
}

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    override val primaryKey = PrimaryKey(id)
}

data class User(
    val id: Long,
    val email: String,
    val name: String,
    val passwordHash: String,
    val createdAt: LocalDateTime
)
```

### 5. Tests

```kotlin
@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var emailService: EmailService

    @InjectMocks
    private lateinit var authService: AuthService

    @Test
    fun `register should create user and return token`() {
        // Arrange
        val dto = RegisterDto(
            email = "john@example.com",
            name = "John",
            password = "password123"
        )
        val savedUser = User(
            id = 1L,
            email = "john@example.com",
            name = "John",
            passwordHash = "hashed",
            createdAt = LocalDateTime.now()
        )
        val token = "jwt.token.here"

        whenever(userRepository.findByEmail(dto.email)).thenReturn(null)
        whenever(passwordEncoder.encode(dto.password)).thenReturn("hashed")
        whenever(userRepository.save(any())).thenReturn(savedUser)
        whenever(jwtProvider.generateToken(1L)).thenReturn(token)

        // Act
        val result = authService.register(dto)

        // Assert
        assertThat(result.token).isEqualTo(token)
        assertThat(result.user.email).isEqualTo("john@example.com")
        verify(emailService).sendWelcomeEmail("john@example.com", "John")
    }

    @Test
    fun `login should return token for valid credentials`() {
        // Arrange
        val dto = LoginDto(
            email = "john@example.com",
            password = "password123"
        )
        val user = User(
            id = 1L,
            email = "john@example.com",
            name = "John",
            passwordHash = "hashed",
            createdAt = LocalDateTime.now()
        )
        val token = "jwt.token.here"

        whenever(userRepository.findByEmail(dto.email)).thenReturn(user)
        whenever(passwordEncoder.matches(dto.password, "hashed")).thenReturn(true)
        whenever(jwtProvider.generateToken(1L)).thenReturn(token)

        // Act
        val result = authService.login(dto)

        // Assert
        assertThat(result.token).isEqualTo(token)
        assertThat(result.user.email).isEqualTo("john@example.com")
    }
}

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        // Clear database
    }

    @Test
    fun `POST auth_register returns 201 with token`() {
        val dto = RegisterDto(
            email = "john@example.com",
            name = "John",
            password = "password123"
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.email").value("john@example.com"))
    }
}
```

### 6. Database Migration

```sql
-- db/migration/V1__init_users.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

---

## Example 2: Product Feature with Pagination

### Controller with Pagination

```kotlin
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {

    @GetMapping
    fun listProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<PageResponse<ProductResponse>> {
        val products = productService.list(page, pageSize, category)
        return ResponseEntity.ok(products)
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): ResponseEntity<ProductResponse> {
        val product = productService.getById(id)
        return ResponseEntity.ok(product.toResponse())
    }

    @PostMapping
    fun createProduct(@Valid @RequestBody dto: CreateProductDto): ResponseEntity<ProductResponse> {
        val product = productService.create(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(product.toResponse())
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @RequestBody dto: UpdateProductDto
    ): ResponseEntity<ProductResponse> {
        val product = productService.update(id, dto)
        return ResponseEntity.ok(product.toResponse())
    }
}
```

### Service with Pagination

```kotlin
@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    fun list(page: Int, pageSize: Int, category: String?): PageResponse<ProductResponse> {
        val result = productRepository.findPaginated(page, pageSize, category)
        return PageResponse(
            content = result.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    fun getById(id: Long): Product {
        return productRepository.findById(id)
            ?: throw ProductNotFoundException("Product $id not found")
    }

    fun create(dto: CreateProductDto): Product {
        val product = Product(
            id = 0L,
            name = dto.name,
            category = dto.category,
            price = dto.price,
            stock = dto.stock,
            createdAt = LocalDateTime.now()
        )
        return productRepository.save(product)
    }

    fun update(id: Long, dto: UpdateProductDto): Product {
        val product = getById(id)
        val updated = product.copy(
            name = dto.name ?: product.name,
            price = dto.price ?: product.price,
            stock = dto.stock ?: product.stock
        )
        return productRepository.save(updated)
    }
}
```

### Repository with Pagination

```kotlin
@Repository
class ProductRepository {

    fun findPaginated(page: Int, pageSize: Int, category: String?): PagedResult<Product> = transaction {
        val query = if (category != null) {
            Products.select { Products.category eq category }
        } else {
            Products.selectAll()
        }

        val total = query.count()
        val totalPages = (total + pageSize - 1) / pageSize

        val products = query
            .orderBy(Products.createdAt to SortOrder.DESC)
            .limit(pageSize, offset = (page * pageSize).toLong())
            .map { it.toProduct() }

        PagedResult(
            content = products,
            totalElements = total,
            totalPages = totalPages,
            currentPage = page
        )
    }

    fun findById(id: Long): Product? = transaction {
        Products.select { Products.id eq id }
            .map { it.toProduct() }
            .firstOrNull()
    }

    fun save(product: Product): Product = transaction {
        val id = if (product.id == 0L) {
            Products.insert {
                it[name] = product.name
                it[category] = product.category
                it[price] = product.price
                it[stock] = product.stock
                it[createdAt] = product.createdAt
            } get Products.id
        } else {
            Products.update({ Products.id eq product.id }) {
                it[name] = product.name
                it[price] = product.price
                it[stock] = product.stock
            }
            product.id
        }
        findById(id)!!
    }

    private fun ResultRow.toProduct() = Product(
        id = this[Products.id],
        name = this[Products.name],
        category = this[Products.category],
        price = this[Products.price],
        stock = this[Products.stock],
        createdAt = this[Products.createdAt]
    )
}
```

### DTOs and Models

```kotlin
data class CreateProductDto(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val category: String,

    @field:DecimalMin("0.01")
    val price: BigDecimal,

    @field:Min(0)
    val stock: Int
)

data class UpdateProductDto(
    val name: String? = null,
    val price: BigDecimal? = null,
    val stock: Int? = null
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val category: String,
    val price: BigDecimal,
    val stock: Int,
    val createdAt: LocalDateTime
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class PagedResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)

data class Product(
    val id: Long,
    val name: String,
    val category: String,
    val price: BigDecimal,
    val stock: Int,
    val createdAt: LocalDateTime
)

fun Product.toResponse() = ProductResponse(
    id = id,
    name = name,
    category = category,
    price = price,
    stock = stock,
    createdAt = createdAt
)

object Products : Table("products") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val category = varchar("category", 100)
    val price = decimal("price", 10, 2)
    val stock = integer("stock")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    override val primaryKey = PrimaryKey(id)
}
```

---

## Common Patterns

### Error Handling Pattern

```kotlin
sealed class AppException(message: String) : RuntimeException(message)
class NotFoundException(id: Long, resource: String) :
    AppException("$resource with ID $id not found")
class ConflictException(message: String) : AppException(message)
class BadRequestException(message: String) : AppException(message)

@RestControllerAdvice
class ExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException) =
        ResponseEntity.notFound().build<Unit>()

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(ex.message ?: "Conflict"))
}
```

### Validation Pattern

```kotlin
@Component
class UserValidator {
    fun validateEmail(email: String) {
        if (!email.contains("@")) throw BadRequestException("Invalid email")
    }

    fun validatePassword(password: String) {
        if (password.length < 8) throw BadRequestException("Password too short")
    }
}
```

### Transaction Pattern

```kotlin
@Transactional
fun complexOperation() {
    val result1 = repository.save(entity1)
    val result2 = repository.save(entity2)
    // If any error, entire transaction rolls back
}
```
