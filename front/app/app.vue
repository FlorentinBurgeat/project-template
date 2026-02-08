<script setup>
import { landingConfig } from '~/landing.config'

useHead({
  meta: [
    { name: 'viewport', content: 'width=device-width, initial-scale=1' }
  ],
  link: [
    { rel: 'icon', href: '/favicon.ico' },
    { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
    { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
    { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap' }
  ],
  htmlAttrs: {
    lang: 'en',
    class: 'scroll-smooth'
  }
})

useSeoMeta({
  title: landingConfig.seo.title,
  description: landingConfig.seo.description,
  ogTitle: landingConfig.seo.title,
  ogDescription: landingConfig.seo.description,
  ogImage: landingConfig.seo.ogImage,
  twitterCard: 'summary_large_image'
})
</script>

<template>
  <UApp>
    <UHeader>
      <template #left>
        <NuxtLink to="/">
          <AppLogo class="w-auto h-6 shrink-0" />
        </NuxtLink>

        <LandingNavLinks />
      </template>

      <template #right>
        <UColorModeButton />

        <UButton
          label="Sign in"
          :to="landingConfig.auth.loginUrl"
          color="neutral"
          variant="ghost"
          size="sm"
        />

        <UButton
          label="Get started"
          :to="landingConfig.auth.registerUrl"
          size="sm"
        />
      </template>
    </UHeader>

    <UMain>
      <NuxtPage />
    </UMain>

    <UFooter>
      <template #left>
        <p class="text-sm text-(--ui-text-muted)">
          {{ landingConfig.footer.copyright }}
        </p>
      </template>

      <template #center>
        <nav class="flex flex-wrap items-center gap-4">
          <NuxtLink
            v-for="link in landingConfig.footer.links"
            :key="link.to"
            :to="link.to"
            class="text-sm text-(--ui-text-muted) hover:text-(--ui-text) transition-colors cursor-pointer"
          >
            {{ link.label }}
          </NuxtLink>
        </nav>
      </template>

      <template #right>
        <LandingSocialLinks :links="landingConfig.footer.socialLinks" />
      </template>
    </UFooter>
  </UApp>
</template>
