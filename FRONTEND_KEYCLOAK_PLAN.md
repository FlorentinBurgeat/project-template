# Frontend Keycloak Integration Plan (Vue 3)

## Overview

This plan outlines the steps to integrate Keycloak authentication into the Vue 3 frontend. The frontend will use the OAuth2 Authorization Code Flow with PKCE, storing access tokens in memory and refresh tokens in HTTP-only cookies.

**Prerequisites:**
- Backend Keycloak integration completed ✅
- Backend running on `http://localhost:8080`
- Keycloak running on `http://localhost:8081`

---

## Architecture Summary

### Authentication Flow

```
┌─────────────┐
│   User      │
│ clicks      │
│ "Login"     │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│ 1. Frontend: GET /api/auth/login                    │
│    ← Backend: { "redirect_url": "http://..." }      │
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│ 2. Frontend: window.location.href = redirect_url    │
│    User redirected to Keycloak login page           │
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│ 3. User enters credentials on Keycloak              │
│    Keycloak validates and authenticates             │
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│ 4. Keycloak: Redirect to /callback?code=ABC123      │
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│ 5. Frontend: POST /api/auth/callback?code=ABC123    │
│    ← Backend: { "access_token": "...", ... }        │
│    ← Backend: Set-Cookie: refresh_token=... HttpOnly│
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│ 6. Frontend: Store access_token in memory           │
│    Navigate to dashboard/home                       │
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│ 7. Frontend: API calls with Authorization header    │
│    Headers: { Authorization: "Bearer <token>" }     │
│    credentials: 'include' (sends refresh cookie)    │
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│ 8. When access token expires (15 min):              │
│    - API returns 401 Unauthorized                   │
│    - Frontend: POST /api/auth/refresh               │
│    - Backend uses refresh_token cookie              │
│    ← New access_token returned                      │
│    - Retry original request                         │
└─────────────────────────────────────────────────────┘
```

### Token Storage Strategy

| Token Type | Storage | Lifetime | Security |
|------------|---------|----------|----------|
| Access Token | In-memory (ref) | 15 minutes | XSS vulnerable but short-lived |
| Refresh Token | HTTP-only cookie | 7 days | XSS-proof, auto-sent by browser |

---

## Step 1: Update Environment Configuration

### File: `front/.env.development`

```env
# Backend API URL
VITE_API_URL=http://localhost:8080

# Frontend callback URL (must match Keycloak client config)
VITE_AUTH_CALLBACK_URL=http://localhost:5173/auth/callback

# Application base URL
VITE_APP_URL=http://localhost:5173
```

### File: `front/.env.production`

```env
VITE_API_URL=https://api.yourapp.com
VITE_AUTH_CALLBACK_URL=https://yourapp.com/auth/callback
VITE_APP_URL=https://yourapp.com
```

---

## Step 2: Install Dependencies (if needed)

```bash
cd front

# If not already installed, add:
# - @tanstack/vue-query (for API calls) - likely already present
# - vue-router (for routing) - likely already present

# No additional OAuth2 library needed - we'll implement it ourselves
```

---

## Step 3: Create Authentication Composable

### File: `front/src/composables/useAuth.ts`

This is the **core authentication logic**. Create a singleton composable that:

**Responsibilities:**
- Stores access token in memory (reactive ref)
- Provides login/logout functions
- Handles token refresh automatically
- Provides authentication state (isAuthenticated, isLoading)
- Persists minimal state across page refreshes

**Key Features:**
- Access token in memory (lost on page refresh → auto-refresh)
- Refresh token in HTTP-only cookie (managed by backend)
- Automatic token refresh on 401 errors
- Loading states for better UX

**Structure:**
```typescript
// Singleton pattern (state shared across app)
export function useAuth() {
  return {
    // State
    accessToken: Ref<string | null>
    isAuthenticated: ComputedRef<boolean>
    isLoading: Ref<boolean>

    // Actions
    login: () => Promise<void>
    register: () => Promise<void>
    handleCallback: (code: string) => Promise<void>
    logout: () => Promise<void>
    refreshToken: () => Promise<void>

    // Utilities
    getAccessToken: () => string | null
  }
}
```

**Implementation Notes:**
- Use `ref()` outside the function for singleton state
- On mount, check if we have a token or need to refresh
- If no token and not logged in, set isAuthenticated = false
- Provide loading state while checking/refreshing

---

## Step 4: Create API Service Layer

### File: `front/src/services/api.ts`

Create a fetch wrapper that:

**Responsibilities:**
- Adds Authorization header automatically
- Includes credentials for cookies
- Handles 401 errors → auto token refresh → retry
- Provides typed request/response helpers

**Key Features:**
```typescript
export async function apiRequest<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  // 1. Get access token from useAuth()
  // 2. Add Authorization header
  // 3. Add credentials: 'include'
  // 4. Make request
  // 5. If 401 → refresh token → retry once
  // 6. Parse response
  // 7. Return typed data
}
```

**Helper Functions:**
```typescript
export const api = {
  get: <T>(url: string) => apiRequest<T>(url, { method: 'GET' }),
  post: <T>(url: string, body?: any) => apiRequest<T>(url, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(url: string, body?: any) => apiRequest<T>(url, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(url: string) => apiRequest<T>(url, { method: 'DELETE' })
}
```

---

## Step 5: Create Authentication Pages

### File: `front/src/pages/auth/LoginPage.vue`

**Responsibilities:**
- Show "Login" and "Register" buttons
- Call `useAuth().login()` on click
- Redirect to Keycloak
- Show loading state

**Template:**
```vue
<template>
  <div class="login-page">
    <h1>Welcome to [App Name]</h1>
    <div v-if="isLoading">
      <p>Redirecting to login...</p>
    </div>
    <div v-else>
      <button @click="handleLogin">Login</button>
      <button @click="handleRegister">Register</button>
    </div>
  </div>
</template>

<script setup lang="ts">
const { login, register, isLoading } = useAuth()

const handleLogin = async () => {
  await login() // Redirects to Keycloak
}

const handleRegister = async () => {
  await register() // Redirects to Keycloak registration
}
</script>
```

### File: `front/src/pages/auth/CallbackPage.vue`

**Responsibilities:**
- Extract authorization code from URL query params
- Call `useAuth().handleCallback(code)`
- Show loading state
- Redirect to home/dashboard on success
- Show error on failure

**Template:**
```vue
<template>
  <div class="callback-page">
    <div v-if="isLoading">
      <p>Completing login...</p>
    </div>
    <div v-else-if="error">
      <p>Login failed: {{ error }}</p>
      <button @click="router.push('/login')">Try Again</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const route = useRoute()
const { handleCallback, isLoading } = useAuth()
const error = ref<string | null>(null)

onMounted(async () => {
  const code = route.query.code as string

  if (!code) {
    error.value = 'No authorization code received'
    return
  }

  try {
    await handleCallback(code)
    // Redirect to home/dashboard
    router.push('/')
  } catch (e) {
    error.value = (e as Error).message
  }
})
</script>
```

---

## Step 6: Update Router Configuration

### File: `front/src/router/index.ts`

**Add Routes:**
```typescript
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/auth/callback',
    name: 'AuthCallback',
    component: () => import('@/pages/auth/CallbackPage.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/pages/HomePage.vue'),
    meta: { requiresAuth: true }
  },
  // ... other routes
]
```

**Add Navigation Guard:**
```typescript
router.beforeEach(async (to, from, next) => {
  const { isAuthenticated, isLoading, refreshToken } = useAuth()

  // Wait for auth check to complete
  if (isLoading.value) {
    // Wait for initial auth check
    // Could use a promise or watch for isLoading to become false
  }

  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !isAuthenticated.value) {
    // Try to refresh token first
    try {
      await refreshToken()
      if (isAuthenticated.value) {
        next()
      } else {
        next('/login')
      }
    } catch {
      next('/login')
    }
  } else if (to.path === '/login' && isAuthenticated.value) {
    // Already logged in, redirect to home
    next('/')
  } else {
    next()
  }
})
```

---

## Step 7: Update Existing Pages

### File: `front/src/pages/HomePage.vue`

**Add Logout Button:**
```vue
<template>
  <div class="home-page">
    <header>
      <h1>Home</h1>
      <button @click="handleLogout">Logout</button>
    </header>
    <!-- ... rest of page -->
  </div>
</template>

<script setup lang="ts">
import { useAuth } from '@/composables/useAuth'

const { logout } = useAuth()

const handleLogout = async () => {
  await logout()
}
</script>
```

### Update API Calls

**Before (old JWT):**
```typescript
const response = await fetch('/api/users/me', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
  }
})
```

**After (Keycloak):**
```typescript
import { api } from '@/services/api'

const user = await api.get<UserResponse>('/api/users/me')
// Authorization header added automatically
// Credentials included automatically
// Token refresh handled automatically
```

---

## Step 8: Update TanStack Query Configuration

### File: `front/src/main.ts` or query config

**Add Default Options:**
```typescript
import { VueQueryPlugin } from '@tanstack/vue-query'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Retry on 401 after token refresh
      retry: (failureCount, error) => {
        if (error.status === 401) {
          return failureCount < 1 // Retry once after refresh
        }
        return failureCount < 3
      },
      // Use our API wrapper
      queryFn: async ({ queryKey }) => {
        const [url] = queryKey
        return api.get(url as string)
      }
    }
  }
})

app.use(VueQueryPlugin, { queryClient })
```

---

## Step 9: Create User Profile Composable (Optional)

### File: `front/src/composables/useUser.ts`

**Fetch current user data:**
```typescript
import { useQuery } from '@tanstack/vue-query'
import { api } from '@/services/api'

export function useUser() {
  const { data: user, isLoading, error, refetch } = useQuery({
    queryKey: ['user', 'me'],
    queryFn: () => api.get<UserResponse>('/api/users/me'),
    enabled: computed(() => useAuth().isAuthenticated.value)
  })

  return {
    user,
    isLoading,
    error,
    refetch
  }
}
```

**Usage in components:**
```vue
<script setup lang="ts">
const { user, isLoading } = useUser()
</script>

<template>
  <div v-if="user">
    <p>Welcome, {{ user.firstName }}!</p>
    <p>Email: {{ user.email }}</p>
  </div>
</template>
```

---

## Step 10: Handle Page Refresh

### Challenge
When the user refreshes the page, the in-memory access token is lost.

### Solution
On app initialization, check if we're authenticated:

**File: `front/src/App.vue`** or **`front/src/main.ts`**

```vue
<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { refreshToken, isLoading } = useAuth()

onMounted(async () => {
  // Try to get a new access token using the refresh_token cookie
  try {
    await refreshToken()
  } catch {
    // No valid refresh token - user needs to log in
  }
})
</script>

<template>
  <div v-if="isLoading">
    <p>Loading...</p>
  </div>
  <RouterView v-else />
</template>
```

---

## Step 11: Add Loading States

### File: `front/src/components/common/LoadingSpinner.vue`

Create a reusable loading component for:
- Initial auth check
- Login redirect
- Callback processing
- Token refresh

**Usage:**
```vue
<LoadingSpinner v-if="isLoading" message="Authenticating..." />
```

---

## Step 12: Error Handling

### File: `front/src/composables/useAuth.ts`

Add error handling for:
- Network errors
- Invalid authorization code
- Expired refresh token
- Server errors

**Strategy:**
```typescript
try {
  await handleCallback(code)
} catch (error) {
  if (error.status === 401) {
    // Invalid code or expired
    showError('Login session expired. Please try again.')
    router.push('/login')
  } else {
    // Other errors
    showError('Login failed. Please try again later.')
  }
}
```

---

## Step 13: Update Header/Navigation Component

### File: `front/src/components/layout/Header.vue`

**Show different UI based on auth state:**
```vue
<template>
  <header>
    <nav>
      <router-link to="/">Home</router-link>

      <div v-if="isAuthenticated">
        <router-link to="/profile">Profile</router-link>
        <button @click="handleLogout">Logout</button>
      </div>
      <div v-else>
        <router-link to="/login">Login</router-link>
      </div>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { useAuth } from '@/composables/useAuth'

const { isAuthenticated, logout } = useAuth()

const handleLogout = async () => {
  await logout()
}
</script>
```

---

## Step 14: TypeScript Types

### File: `front/src/types/auth.ts`

```typescript
export interface TokenResponse {
  access_token: string
  expires_in: number
  token_type: string
}

export interface LoginRedirectResponse {
  redirect_url: string
}

export interface UserResponse {
  id: string
  email: string
  firstName?: string
  lastName?: string
}
```

---

## Testing Checklist

### Manual Testing Steps

1. **Initial Load**
   - [ ] Page loads without errors
   - [ ] Shows login page if not authenticated
   - [ ] Shows home page if refresh token valid

2. **Login Flow**
   - [ ] Click "Login" redirects to Keycloak
   - [ ] Enter credentials on Keycloak
   - [ ] Redirects back to /auth/callback
   - [ ] Exchanges code for tokens
   - [ ] Navigates to home page
   - [ ] Access token stored in memory
   - [ ] Refresh token cookie set (check DevTools → Application → Cookies)

3. **Registration Flow**
   - [ ] Click "Register" redirects to Keycloak registration
   - [ ] Fill out registration form
   - [ ] Redirects back and logs in automatically

4. **Protected Routes**
   - [ ] Can access protected routes when authenticated
   - [ ] Redirects to /login when not authenticated
   - [ ] Navigation guard works correctly

5. **API Calls**
   - [ ] API calls include Authorization header
   - [ ] API calls include credentials (cookies)
   - [ ] 401 errors trigger token refresh
   - [ ] Original request retried after refresh

6. **Token Refresh**
   - [ ] Page refresh triggers automatic token refresh
   - [ ] Expired access token (15 min) auto-refreshes
   - [ ] Failed refresh redirects to login
   - [ ] New access token used for subsequent requests

7. **Logout**
   - [ ] Logout revokes refresh token
   - [ ] Logout clears access token from memory
   - [ ] Logout clears refresh token cookie
   - [ ] Redirects to login page
   - [ ] Cannot access protected routes after logout

8. **Edge Cases**
   - [ ] Direct navigation to /auth/callback without code shows error
   - [ ] Invalid authorization code shows error
   - [ ] Network errors handled gracefully
   - [ ] Multiple tabs sync authentication state (optional, advanced)

---

## Implementation Priority

### Phase 1: Core Authentication (Must Have)
1. Create `useAuth` composable with login/logout/callback
2. Create `api` service wrapper
3. Create LoginPage and CallbackPage
4. Update router with navigation guard
5. Test basic login/logout flow

### Phase 2: Token Management (Must Have)
6. Implement automatic token refresh on 401
7. Handle page refresh (auto token refresh)
8. Add loading states
9. Test token refresh scenarios

### Phase 3: User Experience (Should Have)
10. Update Header/Navigation component
11. Create LoadingSpinner component
12. Add error handling and user feedback
13. Style authentication pages

### Phase 4: Advanced Features (Nice to Have)
14. Create `useUser` composable
15. Add user profile page
16. Implement "Remember me" functionality (longer refresh token)
17. Add multi-tab sync (BroadcastChannel API)

---

## File Structure Summary

```
front/
├── .env.development
├── .env.production
├── src/
│   ├── composables/
│   │   ├── useAuth.ts           ⭐ Core authentication logic
│   │   └── useUser.ts           (Optional) Current user data
│   ├── services/
│   │   └── api.ts               ⭐ API wrapper with auto-refresh
│   ├── pages/
│   │   ├── auth/
│   │   │   ├── LoginPage.vue    ⭐ Login/Register page
│   │   │   └── CallbackPage.vue ⭐ OAuth callback handler
│   │   ├── HomePage.vue         (Updated with logout)
│   │   └── ProfilePage.vue      (Optional)
│   ├── components/
│   │   ├── layout/
│   │   │   └── Header.vue       (Updated with auth state)
│   │   └── common/
│   │       └── LoadingSpinner.vue
│   ├── router/
│   │   └── index.ts             ⭐ Routes + navigation guard
│   ├── types/
│   │   └── auth.ts              TypeScript types
│   ├── App.vue                  (Updated with auth check)
│   └── main.ts                  (Updated with query config)
```

⭐ = Critical files for authentication

---

## Configuration Checklist

Before implementing, verify:

- [ ] Backend is running on http://localhost:8080
- [ ] Keycloak is running on http://localhost:8081
- [ ] Realm is created (check backend logs)
- [ ] Frontend redirect URI matches Keycloak client config
- [ ] CORS is configured correctly in backend
- [ ] Environment variables are set

---

## Common Pitfalls to Avoid

### ❌ Don't Do This:
1. Store access token in localStorage (XSS vulnerable)
2. Store refresh token in JavaScript (defeats HTTP-only security)
3. Forget `credentials: 'include'` (cookies won't be sent)
4. Hard-code API URLs (use environment variables)
5. Retry failed requests infinitely (limit to 1 retry after refresh)
6. Ignore loading states (poor UX during redirects)

### ✅ Do This:
1. Store access token in memory (reactive ref)
2. Let backend manage refresh token (HTTP-only cookie)
3. Always include credentials in fetch requests
4. Use environment variables for URLs
5. Implement smart retry logic (refresh once, then fail)
6. Show loading states during auth operations

---

## Security Best Practices

1. **Never log tokens** in console or analytics
2. **Validate redirect URLs** to prevent open redirects
3. **Use HTTPS in production** (set `secure: true` on cookies)
4. **Implement CSP headers** to prevent XSS
5. **Rotate tokens regularly** (Keycloak handles this)
6. **Clear tokens on logout** (both memory and cookie)
7. **Handle token expiration gracefully**
8. **Don't expose sensitive data** in error messages

---

## Expected API Endpoints (Backend Already Implemented)

| Method | Endpoint | Purpose | Response |
|--------|----------|---------|----------|
| GET | `/api/auth/login` | Get login URL | `{ "redirect_url": "..." }` |
| GET | `/api/auth/register` | Get registration URL | `{ "redirect_url": "..." }` |
| POST | `/api/auth/callback?code=XYZ` | Exchange code for tokens | `{ "access_token": "...", "expires_in": 900 }` + cookie |
| POST | `/api/auth/refresh` | Refresh access token | `{ "access_token": "...", "expires_in": 900 }` + cookie |
| POST | `/api/auth/logout` | Revoke tokens | `204 No Content` |

---

## Next Steps

When you're ready to implement:

1. **Start with Phase 1** (Core Authentication)
   - Focus on getting login/logout working
   - Don't worry about polish initially

2. **Test thoroughly** after each phase
   - Manual testing with browser DevTools
   - Check cookies, headers, network requests

3. **Iterate and improve**
   - Add error handling
   - Improve loading states
   - Style components

4. **Reference backend docs**
   - KEYCLOAK_SETUP.md for backend endpoints
   - MIGRATION_GUIDE.md for API changes

---

## Estimated Implementation Time

- **Phase 1 (Core Auth)**: 2-4 hours
- **Phase 2 (Token Management)**: 2-3 hours
- **Phase 3 (UX)**: 2-3 hours
- **Phase 4 (Advanced)**: 2-4 hours

**Total**: ~8-14 hours for complete implementation

---

## Support Resources

- **Backend Docs**: `back/KEYCLOAK_SETUP.md`
- **Migration Guide**: `back/MIGRATION_GUIDE.md`
- **Keycloak Skill**: `.claude/skills/keycloak/`
- **Frontend Guidelines Skill**: Available in project

---

**Ready to implement when you are!** 🚀

Just provide this plan back when working on the frontend, and we'll implement it step by step.
