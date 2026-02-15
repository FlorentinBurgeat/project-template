// landing.config.ts — Configuration for the landing page structure.
// Text content is now managed via i18n in /i18n/locales/*.json
// This file contains only icons, URLs, and structural configuration.

export interface LandingFeature {
  icon: string
  title: string
  description: string
}

export interface LandingTestimonial {
  name: string
  role: string
  avatar: string
  quote: string
}

export interface LandingNavLink {
  label: string
  to: string
}

export interface LandingSocialLink {
  icon: string
  to: string
  label: string
}

export interface LandingConfig {
  appName: string
  tagline: string
  description: string

  navLinks: LandingNavLink[]

  hero: {
    headline: string
    title: string
    description: string
    primaryCta: { label: string; to: string }
    secondaryCta: { label: string; to: string; icon?: string }
  }

  valueProposition: {
    headline: string
    title: string
    description: string
    points: Array<{ icon: string; text: string }>
  }

  features: {
    headline: string
    title: string
    description: string
    items: LandingFeature[]
  }

  testimonials: {
    headline: string
    title: string
    items: LandingTestimonial[]
  }

  appDownload: {
    headline: string
    title: string
    description: string
    appStoreUrl: string
    googlePlayUrl: string
  }

  faq: {
    headline: string
    title: string
    items: Array<{ label: string; content: string }>
  }

  finalCta: {
    title: string
    description: string
    primaryCta: { label: string; to: string }
    secondaryCta?: { label: string; to: string; icon?: string }
  }

  footer: {
    copyright: string
    links: Array<{ label: string; to: string }>
    socialLinks: LandingSocialLink[]
  }

  seo: {
    title: string
    description: string
    ogImage: string
  }

  auth: {
    loginUrl: string
    registerUrl: string
    accountUrl: string
  }
}

// ──────────────────────────────────────────────
// STRUCTURAL CONFIGURATION (non-translatable)
// Icons, URLs, and other non-text data
// ──────────────────────────────────────────────

export const landingStructure = {
  // Value proposition icons
  valuePropositionIcons: ['i-lucide-clock', 'i-lucide-trending-up', 'i-lucide-lock'],

  // Feature icons
  featureIcons: [
    'i-lucide-zap',
    'i-lucide-shield-check',
    'i-lucide-bar-chart-3',
    'i-lucide-users',
    'i-lucide-smartphone',
    'i-lucide-globe'
  ],

  // Testimonial avatars
  testimonialAvatars: [
    'https://i.pravatar.cc/128?img=1',
    'https://i.pravatar.cc/128?img=2',
    'https://i.pravatar.cc/128?img=3'
  ],

  // App store URLs
  appDownload: {
    appStoreUrl: 'https://apps.apple.com/app/your-app-id',
    googlePlayUrl: 'https://play.google.com/store/apps/details?id=your.app.id'
  },

  // Footer links (to URLs)
  footerLinks: [{ to: '/privacy' }, { to: '/terms' }, { to: 'mailto:contact@example.com' }],

  // Social links
  socialLinks: [
    {
      icon: 'i-simple-icons-x',
      to: 'https://x.com/yourapp'
    },
    {
      icon: 'i-simple-icons-github',
      to: 'https://github.com/yourapp'
    },
    {
      icon: 'i-simple-icons-linkedin',
      to: 'https://linkedin.com/company/yourapp'
    }
  ],

  // SEO
  seo: {
    ogImage: '/og-image.png'
  },

  // Auth URLs
  auth: {
    loginUrl: '/auth/login',
    registerUrl: '/auth/register',
    accountUrl: '/auth/account'
  },

  // Navigation anchor links
  navAnchors: ['#features', '#testimonials', '#download', '#faq'],

  // Hero CTA URLs
  hero: {
    primaryCta: { to: '/auth/register' },
    secondaryCta: { to: '#features', icon: 'i-lucide-arrow-down' }
  },

  // Final CTA URLs
  finalCta: {
    primaryCta: { to: '/auth/register' },
    secondaryCta: { to: 'mailto:sales@example.com', icon: 'i-lucide-mail' }
  }
}

// ──────────────────────────────────────────────
// HELPER FUNCTION: Build full config with i18n
// ──────────────────────────────────────────────

/**
 * Builds the complete landing config by merging structure with i18n translations.
 * Use this in components with: const cfg = useLandingConfig()
 */
export function useLandingConfig(): ComputedRef<LandingConfig> {
  const { t } = useI18n()

  return computed(() => ({
    appName: t('landing.appName'),
    tagline: t('landing.tagline'),
    description: t('landing.description'),

    navLinks: [
      { label: t('landing.nav.features'), to: landingStructure.navAnchors[0]! },
      { label: t('landing.nav.testimonials'), to: landingStructure.navAnchors[1]! },
      { label: t('landing.nav.download'), to: landingStructure.navAnchors[2]! },
      { label: t('landing.nav.faq'), to: landingStructure.navAnchors[3]! }
    ],

    hero: {
      headline: t('landing.hero.headline'),
      title: t('landing.hero.title'),
      description: t('landing.hero.description'),
      primaryCta: {
        label: t('landing.hero.primaryCta'),
        to: landingStructure.hero.primaryCta.to
      },
      secondaryCta: {
        label: t('landing.hero.secondaryCta'),
        to: landingStructure.hero.secondaryCta.to,
        icon: landingStructure.hero.secondaryCta.icon
      }
    },

    valueProposition: {
      headline: t('landing.valueProposition.headline'),
      title: t('landing.valueProposition.title'),
      description: t('landing.valueProposition.description'),
      points: landingStructure.valuePropositionIcons.map((icon, i) => ({
        icon,
        text: t(`landing.valueProposition.points.${i}`)
      }))
    },

    features: {
      headline: t('landing.features.headline'),
      title: t('landing.features.title'),
      description: t('landing.features.description'),
      items: landingStructure.featureIcons.map((icon, i) => ({
        icon,
        title: t(`landing.features.items.${i}.title`),
        description: t(`landing.features.items.${i}.description`)
      }))
    },

    testimonials: {
      headline: t('landing.testimonials.headline'),
      title: t('landing.testimonials.title'),
      items: landingStructure.testimonialAvatars.map((avatar, i) => ({
        avatar,
        name: t(`landing.testimonials.items.${i}.name`),
        role: t(`landing.testimonials.items.${i}.role`),
        quote: t(`landing.testimonials.items.${i}.quote`)
      }))
    },

    appDownload: {
      headline: t('landing.appDownload.headline'),
      title: t('landing.appDownload.title'),
      description: t('landing.appDownload.description'),
      appStoreUrl: landingStructure.appDownload.appStoreUrl,
      googlePlayUrl: landingStructure.appDownload.googlePlayUrl
    },

    faq: {
      headline: t('landing.faq.headline'),
      title: t('landing.faq.title'),
      items: Array.from({ length: 4 }, (_, i) => ({
        label: t(`landing.faq.items.${i}.label`),
        content: t(`landing.faq.items.${i}.content`)
      }))
    },

    finalCta: {
      title: t('landing.finalCta.title'),
      description: t('landing.finalCta.description'),
      primaryCta: {
        label: t('landing.finalCta.primaryCta'),
        to: landingStructure.finalCta.primaryCta.to
      },
      secondaryCta: {
        label: t('landing.finalCta.secondaryCta'),
        to: landingStructure.finalCta.secondaryCta.to,
        icon: landingStructure.finalCta.secondaryCta.icon
      }
    },

    footer: {
      copyright: t('landing.footer.copyright'),
      links: landingStructure.footerLinks.map((link, i) => ({
        label: t(`landing.footer.links.${['privacy', 'terms', 'contact'][i]}`),
        to: link.to
      })),
      socialLinks: landingStructure.socialLinks.map((link, i) => ({
        ...link,
        label: t(`landing.footer.socialLinks.${['twitter', 'github', 'linkedin'][i]}`)
      }))
    },

    seo: {
      title: t('landing.seo.title'),
      description: t('landing.seo.description'),
      ogImage: landingStructure.seo.ogImage
    },

    auth: landingStructure.auth
  }))
}
