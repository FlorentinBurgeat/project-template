# Frontend Keycloak Integration - Quick Start Checklist

**When you're ready to implement the frontend, use this checklist alongside the detailed [FRONTEND_KEYCLOAK_PLAN.md](FRONTEND_KEYCLOAK_PLAN.md).**

---

## Pre-Implementation Checklist

- [ ] Backend is running (`cd back && docker-compose up -d`)
- [ ] Keycloak is accessible at http://localhost:8081/admin
- [ ] Realm is created (check: `docker-compose logs backend | grep "Realm"`)
- [ ] You have the following skills available:
  - `frontend-dev-guidelines` (Vue 3 patterns)
  - `tanstack-query` (data fetching)
  - `tailwindcss` or `shadcn` (styling)

---

## Implementation Checklist

### Phase 1: Core Files (Start Here)

- [ ] **Environment Configuration**
  - [ ] Create `front/.env.development` with:
    ```env
    VITE_API_URL=http://localhost:8080
    VITE_AUTH_CALLBACK_URL=http://localhost:5173/auth/callback
    ```

- [ ] **Core Composable**: `src/composables/useAuth.ts`
  - [ ] Create singleton reactive state (ref outside function)
  - [ ] Implement `login()` - gets redirect URL from `/api/auth/login`
  - [ ] Implement `register()` - gets redirect URL from `/api/auth/register`
  - [ ] Implement `handleCallback(code)` - exchanges code at `/api/auth/callback`
  - [ ] Implement `logout()` - calls `/api/auth/logout`
  - [ ] Implement `refreshToken()` - calls `/api/auth/refresh`
  - [ ] Add `isAuthenticated` computed property
  - [ ] Add `isLoading` ref for async operations

- [ ] **API Service**: `src/services/api.ts`
  - [ ] Create `apiRequest<T>()` wrapper function
  - [ ] Add Authorization header from `useAuth().accessToken`
  - [ ] Add `credentials: 'include'` for cookies
  - [ ] Handle 401 errors → call `refreshToken()` → retry once
  - [ ] Export helper functions: `api.get()`, `api.post()`, etc.

- [ ] **Login Page**: `src/pages/auth/LoginPage.vue`
  - [ ] Add "Login" button → calls `useAuth().login()`
  - [ ] Add "Register" button → calls `useAuth().register()`
  - [ ] Show loading state while redirecting

- [ ] **Callback Page**: `src/pages/auth/CallbackPage.vue`
  - [ ] Extract code from URL query params
  - [ ] Call `useAuth().handleCallback(code)` on mount
  - [ ] Show loading spinner
  - [ ] Navigate to home on success
  - [ ] Show error and retry button on failure

- [ ] **Router**: `src/router/index.ts`
  - [ ] Add `/login` route
  - [ ] Add `/auth/callback` route
  - [ ] Add navigation guard:
    - [ ] Check `isAuthenticated` for protected routes
    - [ ] Try `refreshToken()` if not authenticated
    - [ ] Redirect to `/login` if refresh fails
    - [ ] Redirect to `/` if already logged in and trying to access `/login`

### Phase 2: Integration

- [ ] **Update App.vue or main.ts**
  - [ ] Call `refreshToken()` on mount to check existing session
  - [ ] Show loading spinner until auth check completes

- [ ] **Update Header/Navigation**
  - [ ] Show different UI based on `isAuthenticated`
  - [ ] Add logout button → calls `useAuth().logout()`

- [ ] **Update Existing API Calls**
  - [ ] Replace fetch/axios with `api.get()`, `api.post()`, etc.
  - [ ] Remove manual Authorization headers (now automatic)
  - [ ] Ensure `credentials: 'include'` is used (now automatic)

### Phase 3: Testing

- [ ] **Test Login Flow**
  - [ ] Click "Login" → redirects to Keycloak
  - [ ] Enter credentials → redirects back
  - [ ] Lands on home page authenticated
  - [ ] Access token stored in memory (check component state)
  - [ ] Refresh token cookie set (DevTools → Application → Cookies)

- [ ] **Test Registration Flow**
  - [ ] Click "Register" → redirects to Keycloak registration
  - [ ] Complete registration → redirects back logged in

- [ ] **Test Protected Routes**
  - [ ] Can access home when authenticated
  - [ ] Redirected to login when not authenticated
  - [ ] Navigation guard prevents access

- [ ] **Test Page Refresh**
  - [ ] Refresh page while logged in
  - [ ] Access token auto-refreshed from refresh cookie
  - [ ] Stay logged in after refresh

- [ ] **Test Token Refresh**
  - [ ] Make API call with expired access token (simulate by waiting 15+ min)
  - [ ] API returns 401
  - [ ] Token auto-refreshed
  - [ ] Original request retried successfully

- [ ] **Test Logout**
  - [ ] Click logout
  - [ ] Access token cleared
  - [ ] Refresh token cookie cleared
  - [ ] Redirected to login
  - [ ] Cannot access protected routes

- [ ] **Test Error Cases**
  - [ ] Invalid authorization code
  - [ ] Expired refresh token
  - [ ] Network errors
  - [ ] Each shows appropriate error message

---

## Critical Code Snippets

### useAuth.ts (Skeleton)

```typescript
// Singleton state (outside function)
const accessToken = ref<string | null>(null)
const isLoading = ref(false)

export function useAuth() {
  const isAuthenticated = computed(() => !!accessToken.value)

  const login = async () => {
    isLoading.value = true
    const { redirect_url } = await fetch(`${API_URL}/api/auth/login`).then(r => r.json())
    window.location.href = redirect_url
  }

  const handleCallback = async (code: string) => {
    const { access_token } = await fetch(
      `${API_URL}/api/auth/callback?code=${code}`,
      { method: 'POST', credentials: 'include' }
    ).then(r => r.json())
    accessToken.value = access_token
  }

  const refreshToken = async () => {
    const { access_token } = await fetch(
      `${API_URL}/api/auth/refresh`,
      { method: 'POST', credentials: 'include' }
    ).then(r => r.json())
    accessToken.value = access_token
  }

  const logout = async () => {
    await fetch(`${API_URL}/api/auth/logout`, {
      method: 'POST',
      credentials: 'include'
    })
    accessToken.value = null
  }

  return { accessToken, isAuthenticated, isLoading, login, handleCallback, refreshToken, logout }
}
```

### api.ts (Skeleton)

```typescript
export async function apiRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const { accessToken, refreshToken } = useAuth()

  const makeRequest = async (retry = false): Promise<T> => {
    const response = await fetch(`${API_URL}${endpoint}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken.value}`,
        ...options.headers
      },
      credentials: 'include'
    })

    if (response.status === 401 && !retry) {
      // Refresh and retry once
      await refreshToken()
      return makeRequest(true)
    }

    if (!response.ok) throw new Error(`API Error: ${response.status}`)
    return response.json()
  }

  return makeRequest()
}
```

---

## Common Issues & Solutions

### Issue: "Redirect loop"
**Solution**: Check navigation guard logic - ensure you're not redirecting logged-in users to login

### Issue: "401 Unauthorized"
**Solution**: Check that Authorization header is being sent with correct format: `Bearer <token>`

### Issue: "Cookies not sent"
**Solution**: Ensure `credentials: 'include'` in all fetch requests

### Issue: "CORS error"
**Solution**: Backend CORS must allow your frontend origin with `allowCredentials: true`

### Issue: "Token refresh fails"
**Solution**: Check that refresh token cookie exists (DevTools → Application → Cookies)

### Issue: "Lost auth on page refresh"
**Solution**: Ensure `refreshToken()` is called on app mount (App.vue or main.ts)

---

## Skills to Use During Implementation

When implementing each file, invoke the appropriate skill:

### For useAuth.ts and useUser.ts composables
```
/frontend-dev-guidelines
```
Use for: Composition API patterns, reactive state, singleton composables

### For API service layer
```
/tanstack-query
```
Use for: Data fetching patterns, cache management, query/mutation patterns

### For LoginPage and CallbackPage
```
/shadcn or /tailwindcss
```
Use for: Styling components, forms, buttons, loading states

---

## Time Estimates

- **Setup (env + types)**: 15 minutes
- **useAuth composable**: 1-2 hours
- **api service**: 30-60 minutes
- **Login/Callback pages**: 1-2 hours
- **Router guard**: 30-60 minutes
- **Update existing pages**: 1-2 hours
- **Testing**: 1-2 hours

**Total**: 6-10 hours

---

## Success Criteria

Your implementation is complete when:

✅ User can log in via Keycloak
✅ User can register via Keycloak
✅ User stays logged in on page refresh
✅ Protected routes require authentication
✅ API calls include auth automatically
✅ 401 errors trigger token refresh
✅ User can log out successfully
✅ Error states are handled gracefully

---

## Next Steps

1. **Review the detailed plan**: [FRONTEND_KEYCLOAK_PLAN.md](FRONTEND_KEYCLOAK_PLAN.md)
2. **Start with Phase 1**: Create useAuth, api service, and auth pages
3. **Test each phase**: Don't move forward until current phase works
4. **Use skills**: Invoke relevant skills for each file type
5. **Ask for help**: If stuck, provide specific error messages

---

**Ready to start? Begin with creating `useAuth.ts`!** 🚀
