<script setup lang="ts">
import type { LandingSocialLink } from '~/landing.config'

defineProps<{
  links: LandingSocialLink[]
}>()
</script>

<template>
  <div class="flex items-center gap-2">
    <UButton
      v-for="link in links"
      :key="link.to"
      :to="link.to"
      target="_blank"
      :icon="link.icon"
      :aria-label="link.label"
      color="neutral"
      variant="ghost"
      size="sm"
    />
  </div>
</template>
