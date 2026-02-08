<script setup lang="ts">
import { landingConfig } from '~/landing.config'

const links = landingConfig.navLinks
</script>

<template>
  <nav class="hidden md:flex items-center gap-6">
    <NuxtLink
      v-for="link in links"
      :key="link.to"
      :to="link.to"
      class="text-sm font-medium text-(--ui-text-muted) hover:text-(--ui-text) transition-colors duration-200 cursor-pointer"
    >
      {{ link.label }}
    </NuxtLink>
  </nav>
</template>
