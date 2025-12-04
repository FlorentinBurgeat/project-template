# CLAUDE.md - Frontend

## Architecture Overview

The frontend architecture clearly separates reusable components, business pages, state logic, and data models.

---

## Tech Stack

- **Framework**: Vue 3 (Composition API)
- **Routing**: Vue Router
- **State Management**: Singleton composables (ref/reactive, no Pinia)
- **HTTP Client**: TanStack Query
- **Design System**: ShadCN + Tailwind CSS
- **Build Tool**: Vite

---

## Package Structure

### `/components`
Reusable "dummy" components without business logic. These are generic UI components (buttons, cards, forms, modals, etc.) primarily from ShadCN.

**Characteristics:**
- No business logic
- No direct API calls
- Props-driven
- Emit events for parent handling
- Fully reusable across the application

**Examples:**
```
/components
  /ui
    Button.vue
    Card.vue
    Input.vue
    Modal.vue
```

---

### `/pages`
One page = one business route. Each page contains:
- Main page component
- Page-specific internal components
- Local composables (state management)
- Business logic

**Structure:**
```
/pages
  /home
    HomePage.vue
    /components
      HomeHeader.vue
      HomeMenu.vue
    /composables
      useHomeState.ts
  /settings
    SettingsPage.vue
    /components
      PasswordForm.vue
      EmailForm.vue
    /composables
      useSettingsState.ts
```

**Principles:**
- Each page is self-contained
- Internal components are page-specific and not shared
- Composables manage page state and logic

---

### `/api`
All REST endpoint definitions to the backend. Uses TanStack Query for request management, caching, and mutations.

**Structure:**
```
/api
  auth.ts          - Authentication endpoints
  user.ts          - User management endpoints
  /hooks
    useAuth.ts     - TanStack Query hooks for auth
    useUser.ts     - TanStack Query hooks for user
```

**Example:**
```typescript
// api/auth.ts
export const authApi = {
  login: (credentials: LoginDTO) => axios.post('/auth/login', credentials),
  register: (data: RegisterDTO) => axios.post('/auth/register', data),
  refresh: (token: string) => axios.post('/auth/refresh', { token })
}

// api/hooks/useAuth.ts
export const useLogin = () => {
  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      // Store tokens
    }
  })
}
```

---

### `/model`
All business models, DTOs, and their mappers to transform data between frontend and backend.

**Structure:**
```
/model
  User.ts
  UserDTO.ts
  Todo.ts
  TodoDTO.ts
  /mappers
    userMapper.ts
    todoMapper.ts
```

**Purpose:**
- **Models**: Internal representation used in the application
- **DTOs**: Data Transfer Objects matching backend API contracts
- **Mappers**: Transform DTOs to/from Models

---

## State Management: Singleton Composables

State is managed via **singleton composables** without Pinia. A singleton composable defines `ref()` or `reactive()` outside the composable function, making state shared across all instances.

**Example:**
```typescript
// composables/useAuthState.ts
import { ref } from 'vue'

// State defined OUTSIDE the function = singleton
const user = ref<User | null>(null)
const isAuthenticated = ref(false)

export const useAuthState = () => {
  const login = (userData: User) => {
    user.value = userData
    isAuthenticated.value = true
  }

  const logout = () => {
    user.value = null
    isAuthenticated.value = false
  }

  return {
    user: readonly(user),
    isAuthenticated: readonly(isAuthenticated),
    login,
    logout
  }
}
```

**When to use:**
- Global state (authentication, user preferences)
- State shared between multiple pages
- Application-wide configuration

**When NOT to use:**
- Local page state → use regular composables inside page
- Component-specific state → use local refs in component

---

## Routing

Vue Router manages navigation between pages.

**Route Structure:**
```typescript
const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/home/HomePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/auth/LoginPage.vue')
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/pages/settings/SettingsPage.vue'),
    meta: { requiresAuth: true }
  }
]
```

**Route Guards:**
- Global guard checks `meta.requiresAuth`
- Redirects to login if not authenticated
- Validates JWT token before allowing access

---

## Styling: ShadCN + Tailwind

- **ShadCN**: Provides pre-built, accessible UI components
- **Tailwind CSS**: Utility-first CSS framework
- Components in `/components/ui` are ShadCN components
- Custom styling uses Tailwind utility classes

**Principles:**
- Use ShadCN components as base
- Customize with Tailwind utilities
- Maintain consistent design system
- Avoid custom CSS when possible

---

## Base Pages Included

### Login/Register Page
- Email/password form
- Form validation
- Error handling
- Redirect after successful login

### Home Page
- Header with navigation
- Menu (sidebar or top navigation)
- Protected route (requires authentication)

### Settings Page
- Change password form
- Change email form
- Delete account action
- Form validation and feedback

---

## Adding a New Page

### Steps
1. Create folder in `/pages/page-name`
2. Create main page component `PageName.vue`
3. Create internal components in `/components` subfolder if needed
4. Create composables in `/composables` subfolder for state management
5. Add API endpoints in `/api` if needed
6. Create models/DTOs in `/model` if needed
7. Add route in router configuration

### Example
```
/pages/profile
  ProfilePage.vue
  /components
    ProfileHeader.vue
    ProfileStats.vue
  /composables
    useProfileState.ts
```

---

## Skills Reference

This section lists the available skills for frontend development tasks.

### Available Skills
<!-- To be completed with actual skills -->
- TBD

---

**Version**: 1.0  
**Last Updated**: December 2024