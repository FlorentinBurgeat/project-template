<template>
  <div class="rounded-lg border border-(--ui-border) bg-(--ui-bg) p-6 flex flex-col gap-4">
    <p class="text-(--ui-text-muted) italic leading-relaxed">"{{ testimonial.quote }}"</p>
    <div class="flex items-center gap-3 mt-auto">
      <UAvatar :src="testimonial.avatar" :alt="testimonial.name" size="md" />
      <div>
        <p class="font-semibold text-sm">{{ testimonial.name }}</p>
        <p class="text-xs text-(--ui-text-dimmed)">{{ testimonial.role }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LandingTestimonial } from '~/landing.config'

defineProps<{
  testimonial: LandingTestimonial
}>()
</script>
