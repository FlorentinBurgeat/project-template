# Server API Routes Reference

## API Routes (`server/api/`)

All routes in `server/api/` are automatically prefixed with `/api/`.

### Basic API Route

```typescript
// server/api/users.ts → /api/users
export default defineEventHandler(async (event) => {
  // GET all users
  return { users: [] }
})
```

### Dynamic Route Parameters

```typescript
// server/api/users/[id].ts → /api/users/:id
export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')
  return { user: { id } }
})
```

### Multiple HTTP Methods

```typescript
// server/api/users/[id].ts
export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')

  if (event.method === 'GET') {
    return { user: { id } }
  }

  if (event.method === 'PUT') {
    const body = await readBody(event)
    return { updated: true, data: body }
  }

  if (event.method === 'DELETE') {
    return { deleted: true, id }
  }

  throw createError({
    statusCode: 405,
    message: 'Method not allowed'
  })
})
```

### Authentication Example

```typescript
// server/api/auth/login.ts → /api/auth/login
export default defineEventHandler(async (event) => {
  const body = await readBody(event)

  // Validate credentials
  const user = await validateCredentials(body)

  if (!user) {
    throw createError({
      statusCode: 401,
      message: 'Invalid credentials'
    })
  }

  // Set auth cookie
  setCookie(event, 'auth_token', user.token, {
    httpOnly: true,
    secure: true,
    maxAge: 60 * 60 * 24 * 7 // 7 days
  })

  return { user }
})
```

## Server Routes (`server/routes/`)

Custom routes without `/api/` prefix.

```typescript
// server/routes/health.ts → /health
export default defineEventHandler(() => {
  return { status: 'ok', timestamp: Date.now() }
})
```

## Server Middleware (`server/middleware/`)

Runs on every request after Nitro initialization.

### Authentication Middleware

```typescript
// server/middleware/auth.ts
export default defineEventHandler((event) => {
  const token = getCookie(event, 'auth_token')

  if (!token && !event.path.startsWith('/api/public')) {
    throw createError({
      statusCode: 401,
      message: 'Unauthorized'
    })
  }
})
```

### Logging Middleware

```typescript
// server/middleware/log.ts
export default defineEventHandler((event) => {
  console.log(`${event.method} ${event.path}`)
})
```

## Nitro Utilities Reference

### Request Handling

```typescript
// Read request body
const body = await readBody(event)

// Get query parameters
const query = getQuery(event)
// Example: /api/users?page=1&limit=10
// query = { page: '1', limit: '10' }

// Get route params
const id = getRouterParam(event, 'id')

// Get headers
const auth = getHeader(event, 'authorization')

// Get request method
const method = event.method // 'GET', 'POST', etc.

// Get request path
const path = event.path
```

### Response Handling

```typescript
// Set response headers
setHeader(event, 'x-custom', 'value')

// Set response status
setResponseStatus(event, 201)

// Return JSON (automatic)
return { data: 'value' }

// Return text
return 'Plain text response'

// Send file
return sendStream(event, fs.createReadStream('/path/to/file'))
```

### Cookie Management

```typescript
// Get cookie
const token = getCookie(event, 'token')

// Set cookie
setCookie(event, 'token', 'value', {
  httpOnly: true,
  secure: true,
  maxAge: 60 * 60 * 24 * 7, // 7 days
  path: '/',
  sameSite: 'lax'
})

// Delete cookie
deleteCookie(event, 'token')
```

### Error Handling

```typescript
// Throw HTTP error
throw createError({
  statusCode: 404,
  message: 'Not found'
})

// With additional data
throw createError({
  statusCode: 400,
  message: 'Validation failed',
  data: { field: 'email', error: 'Invalid format' }
})
```

## Error Handling Patterns

### Try-Catch Pattern

```typescript
// server/api/users.ts
export default defineEventHandler(async (event) => {
  try {
    const users = await fetchUsers()
    return { users }
  } catch (error) {
    throw createError({
      statusCode: 500,
      message: 'Failed to fetch users',
      cause: error
    })
  }
})
```

### Validation Pattern

```typescript
// server/api/users.ts
export default defineEventHandler(async (event) => {
  const body = await readBody(event)

  // Validate input
  if (!body.email || !body.password) {
    throw createError({
      statusCode: 400,
      message: 'Email and password are required'
    })
  }

  if (!isValidEmail(body.email)) {
    throw createError({
      statusCode: 400,
      message: 'Invalid email format'
    })
  }

  // Process request
  return await createUser(body)
})
```

## API Proxy Pattern

Proxy requests to external backend:

```typescript
// server/api/proxy/[...path].ts
export default defineEventHandler(async (event) => {
  const path = event.path.replace('/api/proxy', '')
  const config = useRuntimeConfig()

  return $fetch(`${config.backendUrl}${path}`, {
    method: event.method,
    headers: getHeaders(event),
    body: event.method !== 'GET' ? await readBody(event) : undefined
  })
})
```

## Complete CRUD Example

```typescript
// server/api/todos/[id].ts
export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')

  // GET /api/todos/:id
  if (event.method === 'GET') {
    const todo = await db.todos.findById(id)
    if (!todo) {
      throw createError({
        statusCode: 404,
        message: 'Todo not found'
      })
    }
    return { todo }
  }

  // PUT /api/todos/:id
  if (event.method === 'PUT') {
    const body = await readBody(event)
    const todo = await db.todos.update(id, body)
    return { todo }
  }

  // DELETE /api/todos/:id
  if (event.method === 'DELETE') {
    await db.todos.delete(id)
    return { success: true }
  }

  throw createError({
    statusCode: 405,
    message: 'Method not allowed'
  })
})
```
