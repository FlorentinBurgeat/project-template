<template>
  <UDropdownMenu
    :items="menuItems"
    :portal="true"
    :content="{ side: 'bottom', align: 'end', sideOffset: 8, positionStrategy: 'fixed' }"
  >
    <template #item="{ item }">
      <NuxtLink :to="switchLocalePath(item.code as Locale)" class="flex items-center gap-2 w-full">
        <span class="text-base">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </NuxtLink>
    </template>

    <UButton color="neutral" variant="ghost" size="sm" trailing-icon="i-lucide-chevron-down">
      <template #leading>
        <span class="text-base">{{ currentLocaleIcon }}</span>
      </template>
      {{ currentLocaleName }}
    </UButton>
  </UDropdownMenu>
</template>

<script setup lang="ts">
import type { Locale } from '~~/i18n/i18n.domain'

const { locale, locales } = useI18n()
const switchLocalePath = useSwitchLocalePath()

const currentLocaleName = computed(() => {
  const current = (locales.value as Array<{ code: string; name: string }>).find(
    (loc) => loc.code === locale.value
  )
  return current?.name || ''
})

const currentLocaleIcon = computed(() => getLocaleIcon(locale.value))

function getLocaleIcon(code: string) {
  const icons: Record<string, string> = {
    en: '🇬🇧',
    fr: '🇫🇷'
  }
  return icons[code] || '🌐'
}

const menuItems = computed(() => {
  const allLocales = locales.value as Array<{ code: string; name: string }>

  return allLocales.map((loc) => ({
    label: loc.name,
    icon: getLocaleIcon(loc.code),
    disabled: loc.code === locale.value,
    code: loc.code
  }))
})
</script>
