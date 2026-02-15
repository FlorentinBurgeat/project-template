<template>
  <UPage>
    <UPageHeader
      :title="$t('dashboard.title')"
      :description="
        user ? $t('dashboard.welcome', { name: user.given_name || user.name || user.email }) : ''
      "
    />

    <UPageBody>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <UPageCard
          :title="$t('dashboard.cards.profile.title')"
          :description="$t('dashboard.cards.profile.description')"
          icon="i-lucide-user"
        />

        <UPageCard
          :title="$t('dashboard.cards.settings.title')"
          :description="$t('dashboard.cards.settings.description')"
          icon="i-lucide-settings"
        />

        <UPageCard
          :title="$t('dashboard.cards.activity.title')"
          :description="$t('dashboard.cards.activity.description')"
          icon="i-lucide-activity"
        />
      </div>
    </UPageBody>
  </UPage>
</template>

<script setup lang="ts">
definePageMeta({
  middleware: 'auth'
})

const { user } = useAuth()
const { t } = useI18n()

useSeoMeta({
  title: () => t('dashboard.seo.title'),
  description: () => t('dashboard.seo.description')
})
</script>
