<template>
  <div>
    <!-- HERO -->
    <UPageHero
    :headline="cfg.hero.headline"
      :title="cfg.hero.title"
      :description="cfg.hero.description"
      :links="[
        {
          label: cfg.hero.primaryCta.label,
          to: cfg.hero.primaryCta.to,
          trailingIcon: 'i-lucide-arrow-right',
          size: 'xl' as const
        },
        {
          label: cfg.hero.secondaryCta.label,
          to: cfg.hero.secondaryCta.to,
          icon: cfg.hero.secondaryCta.icon,
          size: 'xl' as const,
          color: 'neutral' as const,
          variant: 'subtle' as const
        }
      ]"
    />

    <!-- VALUE PROPOSITION -->
    <UPageSection
      id="value"
      :headline="cfg.valueProposition.headline"
      :title="cfg.valueProposition.title"
      :description="cfg.valueProposition.description"
    >
      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div
          v-for="(point, i) in cfg.valueProposition.points"
          :key="i"
          class="flex items-start gap-3"
        >
          <UIcon
          :name="point.icon"
          class="text-primary size-6 shrink-0 mt-0.5"
          />
          <p class="text-(--ui-text-muted)">
            {{ point.text }}
          </p>
        </div>
      </div>
    </UPageSection>

    <!-- FEATURES -->
    <UPageSection
    id="features"
    :headline="cfg.features.headline"
    :title="cfg.features.title"
    :description="cfg.features.description"
    :features="cfg.features.items"
    />

    <!-- TESTIMONIALS -->
    <UPageSection
    id="testimonials"
    :headline="cfg.testimonials.headline"
    :title="cfg.testimonials.title"
    >
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <LandingTestimonialCard
        v-for="(testimonial, i) in cfg.testimonials.items"
          :key="i"
          :testimonial="testimonial"
        />
      </div>
    </UPageSection>

    <!-- APP DOWNLOAD -->
    <UPageSection
    id="download"
      :headline="cfg.appDownload.headline"
      :title="cfg.appDownload.title"
      :description="cfg.appDownload.description"
      >
      <LandingAppBadges
      :app-store-url="cfg.appDownload.appStoreUrl"
      :google-play-url="cfg.appDownload.googlePlayUrl"
      />
    </UPageSection>

    <!-- FAQ -->
    <UPageSection
    id="faq"
    :headline="cfg.faq.headline"
    :title="cfg.faq.title"
    >
      <UAccordion
      :items="cfg.faq.items"
      class="max-w-2xl mx-auto"
      />
    </UPageSection>

    <!-- FINAL CTA -->
    <UPageSection>
      <UPageCTA
      :title="cfg.finalCta.title"
      :description="cfg.finalCta.description"
      variant="subtle"
      :links="[
        {
          label: cfg.finalCta.primaryCta.label,
            to: cfg.finalCta.primaryCta.to,
            trailingIcon: 'i-lucide-arrow-right',
            color: 'neutral' as const
          },
          ...(cfg.finalCta.secondaryCta ? [{
            label: cfg.finalCta.secondaryCta.label,
            to: cfg.finalCta.secondaryCta.to,
            icon: cfg.finalCta.secondaryCta.icon,
            color: 'neutral' as const,
            variant: 'outline' as const
          }] : [])
        ]"
      />
    </UPageSection>
  </div>
</template>

          <script setup lang="ts">
          import { landingConfig } from '~/landing.config'
          
          const cfg = landingConfig
          </script>