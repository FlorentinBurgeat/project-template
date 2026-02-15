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
        <LanguageSwitcher />
        <UColorModeButton />

        <UButton
          :label="$t('landing.header.signIn')"
          :to="landingStructure.auth.loginUrl"
          color="neutral"
          variant="ghost"
          size="sm"
        />

        <UButton
          :label="$t('landing.header.getStarted')"
          :to="landingStructure.auth.registerUrl"
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
          {{ $t('landing.footer.copyright') }}
        </p>
      </template>

      <template #center>
        <nav class="flex flex-wrap items-center gap-4">
          <NuxtLink
            v-for="(link, key) in landingStructure.footerLinks"
            :key="link.to"
            :to="link.to"
            class="text-sm text-(--ui-text-muted) hover:text-(--ui-text) transition-colors cursor-pointer"
          >
            {{ $t(`landing.footer.links.${['privacy', 'terms', 'contact'][key]}`) }}
          </NuxtLink>
        </nav>
      </template>

      <template #right>
        <LandingSocialLinks :links="landingStructure.socialLinks" />
      </template>
    </UFooter>
  </UApp>
</template>

<script setup>
import { landingStructure } from '~/landing.config'

const { t, locale } = useI18n()

useHead({
  meta: [{ name: 'viewport', content: 'width=device-width, initial-scale=1' }],
  link: [
    { rel: 'icon', href: '/favicon.ico' },
    { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
    { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
    {
      rel: 'stylesheet',
      href: 'https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap'
    }
  ],
  htmlAttrs: {
    lang: locale.value,
    class: 'scroll-smooth'
  }
})

useSeoMeta({
  title: () => t('landing.seo.title'),
  description: () => t('landing.seo.description'),
  ogTitle: () => t('landing.seo.title'),
  ogDescription: () => t('landing.seo.description'),
  ogImage: landingStructure.seo.ogImage,
  twitterCard: 'summary_large_image'
})
</script>
