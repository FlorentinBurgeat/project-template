<script setup lang="ts">
defineProps<{
  appStoreUrl: string
  googlePlayUrl: string
}>()
</script>

<template>
  <div class="flex flex-wrap items-center gap-4">
    <NuxtLink
      v-if="appStoreUrl"
      :to="appStoreUrl"
      target="_blank"
      external
      aria-label="Download on the App Store"
      class="cursor-pointer"
    >
      <img
        src="/images/badge-app-store.svg"
        alt="Download on the App Store"
        class="h-12"
        loading="lazy"
      >
    </NuxtLink>
    <NuxtLink
      v-if="googlePlayUrl"
      :to="googlePlayUrl"
      target="_blank"
      external
      aria-label="Get it on Google Play"
      class="cursor-pointer"
    >
      <img
        src="/images/badge-google-play.svg"
        alt="Get it on Google Play"
        class="h-12"
        loading="lazy"
      >
    </NuxtLink>
  </div>
</template>
