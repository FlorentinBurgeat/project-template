// @ts-check
import withNuxt from './.nuxt/eslint.config.mjs'
import eslingPluginPrettierRecommended from 'eslint-plugin-prettier/recommended'
export default withNuxt([
  eslingPluginPrettierRecommended,
  // Your custom configs here
  {
    rules: {
      '@stylistic/semi': 'off',
      '@stylistic/quotes': 'off',
      '@stylistic/comma-dangle': 'off',
      '@stylistic/indent': 'off',
      'vue/html-self-closing': 'always'
    }
  }
])
