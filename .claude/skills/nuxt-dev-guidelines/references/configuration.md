# Configuration Reference

## nuxt.config.ts

Primary configuration file for Nuxt applications.

### Complete Example

```typescript
export default defineNuxtConfig({
  // Compatibility date (required for Nuxt 4)
  compatibilityDate: '2025-01-15',

  // Future compatibility
  future: {
    compatibilityVersion: 4
  },

  // Source directory
  srcDir: 'app/',
  serverDir: 'server/',

  // Rendering mode
  ssr: true,

  // Modules
  modules: [
    '@nuxt/eslint',
    '@nuxt/ui',
    '@nuxt/a11y',
    '@nuxt/hints',
    '@nuxt/image',
    '@nuxt/test-utils'
  ],

  // Dev tools
  devtools: {
    enabled: true
  },

  // Global CSS
  css: ['~/assets/css/main.css'],

  // Route rules
  routeRules: {
    '/': { prerender: true },
    '/admin/**': { ssr: false },
    '/api/**': { cors: true },
    '/old-page': { redirect: '/new-page' }
  },

  // Runtime config (env variables)
  runtimeConfig: {
    // Private (server-only)
    apiSecret: process.env.API_SECRET,
    databaseUrl: process.env.DATABASE_URL,

    // Public (client + server)
    public: {
      apiBase: process.env.API_BASE_URL || 'http://localhost:3000',
      appName: 'My App'
    }
  },

  // Auto-imports configuration
  imports: {
    dirs: ['stores', 'utils']
  },

  // TypeScript
  typescript: {
    strict: true,
    typeCheck: true
  },

  // Nitro configuration
  nitro: {
    preset: 'node-server',
    compressPublicAssets: true
  },

  // Vite configuration
  vite: {
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: '@use "~/assets/styles/variables.scss" as *;'
        }
      }
    }
  }
})
```

### Key Configuration Options

#### Directory Structure

```typescript
{
  srcDir: 'app/',           // Source directory
  serverDir: 'server/',     // Server directory
  buildDir: '.nuxt/',       // Build output directory
  dir: {
    pages: 'pages',         // Pages directory
    layouts: 'layouts',     // Layouts directory
    middleware: 'middleware',
    plugins: 'plugins',
    public: 'public',       // Static assets
    assets: 'assets'        // Build-time assets
  }
}
```

#### Rendering Modes

```typescript
{
  ssr: true,              // Enable SSR
  // OR
  ssr: false,             // SPA mode

  // Route-specific rendering
  routeRules: {
    '/': { prerender: true },        // SSG (Static)
    '/admin/**': { ssr: false },     // SPA
    '/api/**': { cors: true }        // API routes
  }
}
```

#### Runtime Configuration

```typescript
{
  runtimeConfig: {
    // Server-only (never exposed to client)
    apiSecret: process.env.API_SECRET,

    // Public (available on both server and client)
    public: {
      apiBase: process.env.API_BASE_URL
    }
  }
}
```

**Access runtime config:**

```typescript
// In any component or composable
const config = useRuntimeConfig()

// Server-side: access both private and public
const secret = config.apiSecret
const apiBase = config.public.apiBase

// Client-side: only public accessible
const apiBase = config.public.apiBase
```

#### Auto-Imports

```typescript
{
  imports: {
    // Auto-import from additional directories
    dirs: ['stores', 'utils', 'types'],

    // Disable auto-imports
    autoImport: false,

    // Add custom imports
    presets: [
      {
        from: 'vue-i18n',
        imports: ['useI18n']
      }
    ]
  }
}
```

## app.config.ts

Reactive app-level configuration exposed to the application.

### Basic Usage

```typescript
// app.config.ts
export default defineAppConfig({
  // UI theme
  ui: {
    primary: 'green',
    gray: 'slate'
  },

  // App settings
  theme: {
    darkMode: true
  },

  // Custom config
  footer: {
    links: [
      { label: 'About', to: '/about' },
      { label: 'Contact', to: '/contact' }
    ]
  }
})
```

### Access in Components

```vue
<script setup lang="ts">
const appConfig = useAppConfig()

// Access config
const primaryColor = appConfig.ui.primary
const darkMode = appConfig.theme.darkMode
const footerLinks = appConfig.footer.links
</script>
```

### Difference: runtimeConfig vs app.config

| Feature | `runtimeConfig` | `app.config` |
|---------|----------------|--------------|
| Usage | Environment variables, secrets | App settings, theme config |
| When defined | Build time (from .env) | Build time (hardcoded) |
| Reactive | No | Yes |
| Server-only values | Yes (private keys) | No |
| Typical use | API keys, DB URLs | UI theme, app settings |

## Environment Variables

### Setup

```bash
# .env
API_SECRET=your-secret-key
API_BASE_URL=http://localhost:3000
DATABASE_URL=postgresql://...
```

### Access in nuxt.config.ts

```typescript
export default defineNuxtConfig({
  runtimeConfig: {
    apiSecret: process.env.API_SECRET,
    databaseUrl: process.env.DATABASE_URL,

    public: {
      apiBase: process.env.API_BASE_URL
    }
  }
})
```

### Access in Application

```typescript
const config = useRuntimeConfig()
const apiSecret = config.apiSecret // Server-only
const apiBase = config.public.apiBase // Client + Server
```

## Route Rules

Configure route-specific behavior.

```typescript
{
  routeRules: {
    // Static generation
    '/': { prerender: true },

    // SPA mode for specific routes
    '/admin/**': { ssr: false },

    // ISR (Incremental Static Regeneration)
    '/blog/**': { swr: 3600 }, // Revalidate every hour

    // Redirects
    '/old-page': { redirect: '/new-page' },
    '/old/**': { redirect: '/new/**' },

    // Headers
    '/api/**': {
      cors: true,
      headers: {
        'access-control-allow-methods': 'GET,POST'
      }
    },

    // Cache control
    '/images/**': {
      headers: {
        'cache-control': 'max-age=31536000'
      }
    }
  }
}
```

## TypeScript Configuration

### Nuxt Config

```typescript
{
  typescript: {
    strict: true,        // Enable strict mode
    typeCheck: true,     // Type check on build
    shim: true           // Generate shims for .vue files
  }
}
```

### tsconfig.json

```json
{
  "extends": "./.nuxt/tsconfig.json",
  "compilerOptions": {
    "strict": true,
    "types": [
      "@nuxt/types",
      "@types/node"
    ]
  }
}
```

## Module Configuration

### Install Module

```bash
npm install @nuxtjs/tailwindcss
```

### Add to Config

```typescript
{
  modules: [
    '@nuxtjs/tailwindcss'
  ],

  // Module-specific config
  tailwindcss: {
    cssPath: '~/assets/css/tailwind.css',
    configPath: 'tailwind.config.js'
  }
}
```

## Build Configuration

```typescript
{
  // Build optimization
  build: {
    transpile: ['some-package']
  },

  // Nitro build config
  nitro: {
    preset: 'node-server',
    compressPublicAssets: true,
    minify: true,
    sourceMap: false
  },

  // Vite config
  vite: {
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            // Custom chunking strategy
          }
        }
      }
    }
  }
}
```

## Development Configuration

```typescript
{
  // Dev server
  devServer: {
    port: 3000,
    host: '0.0.0.0'
  },

  // Dev tools
  devtools: {
    enabled: true,
    timeline: {
      enabled: true
    }
  }
}
```
