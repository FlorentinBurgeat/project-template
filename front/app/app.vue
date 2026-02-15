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

        <template v-if="!isAuthenticated">
          <UButton
            :label="$t('landing.header.signIn')"
            color="neutral"
            variant="ghost"
            size="sm"
            @click="login"
          />

          <UButton :label="$t('landing.header.getStarted')" size="sm" @click="register" />
        </template>

        <template v-else>
          <UDropdownMenu :items="userMenuItems">
            <UButton
              :label="user?.given_name || user?.name || user?.email || ''"
              color="neutral"
              variant="ghost"
              size="sm"
              icon="i-lucide-user"
              trailing-icon="i-lucide-chevron-down"
            />
          </UDropdownMenu>
        </template>
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
const { isAuthenticated, user, login, register, logout } = useAuth()

const userMenuItems = computed(() => [
  [
    {
      label: t('auth.menu.dashboard'),
      icon: 'i-lucide-layout-dashboard',
      onSelect: () => navigateTo('/dashboard')
    },
    {
      label: t('auth.menu.settings'),
      icon: 'i-lucide-settings',
      onSelect: () => navigateTo('/dashboard')
    }
  ],
  [
    {
      label: t('auth.menu.logout'),
      icon: 'i-lucide-log-out',
      onSelect: () => logout()
    }
  ]
])

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
