// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  modules: [
    '@nuxt/eslint',
    '@nuxt/ui',
    '@nuxt/a11y',
    '@nuxt/hints',
    '@nuxt/image',
    '@nuxt/test-utils',
    '@nuxtjs/i18n'
  ],

  devtools: {
    enabled: true
  },

  runtimeConfig: {
    // Backend base URL used by the server-side /api proxy (server/api/[...path].ts).
    // Server-only; override at runtime with NUXT_API_BASE_URL.
    apiBaseUrl: 'http://localhost:8080'
  },

  css: ['~/assets/css/main.css'],

  routeRules: {
    '/': { prerender: true }
  },

  compatibilityDate: '2025-01-15',

  eslint: {
    config: {
      stylistic: false // Disabled - using oxfmt for formatting
    }
  },

  i18n: {
    defaultLocale: 'en',
    locales: [
      { code: 'en', name: 'English', file: 'en.json', language: 'en-US' },
      { code: 'fr', name: 'Français', file: 'fr.json', language: 'fr-FR' }
    ],
    langDir: 'locales',
    strategy: 'prefix',
    detectBrowserLanguage: false
  }
})
