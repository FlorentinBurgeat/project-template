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
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'
    }
  },

  css: ['~/assets/css/main.css'],

  routeRules: {
    '/': { prerender: true },
    '/api/**': {
      proxy: { to: (process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080') + '/api/**' }
    }
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
    lazy: true,
    strategy: 'prefix',
    detectBrowserLanguage: false
  }
})
