// landing.config.ts — Single source of truth for the landing page.
// To customize this boilerplate for a new project, edit the values below.
// See LANDING_SETUP.md for a step-by-step guide.

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
    primaryCta: { label: string, to: string }
    secondaryCta: { label: string, to: string, icon?: string }
  }

  valueProposition: {
    headline: string
    title: string
    description: string
    points: Array<{ icon: string, text: string }>
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
    items: Array<{ label: string, content: string }>
  }

  finalCta: {
    title: string
    description: string
    primaryCta: { label: string, to: string }
    secondaryCta?: { label: string, to: string, icon?: string }
  }

  footer: {
    copyright: string
    links: Array<{ label: string, to: string }>
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

export const landingConfig: LandingConfig = {
  // ──────────────────────────────────────────────
  // BRANDING — Replace with your app identity
  // ──────────────────────────────────────────────
  appName: 'AppName',
  tagline: 'Your tagline goes here',
  description: 'A short description of what your application does.',

  // ──────────────────────────────────────────────
  // NAVIGATION — Anchor links in the header
  // ──────────────────────────────────────────────
  navLinks: [
    { label: 'Features', to: '#features' },
    { label: 'Testimonials', to: '#testimonials' },
    { label: 'Download', to: '#download' },
    { label: 'FAQ', to: '#faq' }
  ],

  // ──────────────────────────────────────────────
  // HERO — First thing visitors see
  // ──────────────────────────────────────────────
  hero: {
    headline: 'Now available',
    title: 'The modern platform for [your use case]',
    description: 'Replace this with a compelling description of your product. Explain the core value proposition in one or two sentences.',
    primaryCta: { label: 'Get started', to: '/auth/register' },
    secondaryCta: { label: 'Learn more', to: '#features', icon: 'i-lucide-arrow-down' }
  },

  // ──────────────────────────────────────────────
  // VALUE PROPOSITION — Why users should care
  // ──────────────────────────────────────────────
  valueProposition: {
    headline: 'Why choose us',
    title: 'Solve [problem] without [pain point]',
    description: 'Describe the core problem your users face and how your product solves it differently.',
    points: [
      { icon: 'i-lucide-clock', text: 'Save hours every week on [task]' },
      { icon: 'i-lucide-trending-up', text: 'Increase [metric] significantly' },
      { icon: 'i-lucide-lock', text: 'Enterprise-grade [benefit]' }
    ]
  },

  // ──────────────────────────────────────────────
  // FEATURES — What the product does
  // ──────────────────────────────────────────────
  features: {
    headline: 'Features',
    title: 'Everything you need to [achieve goal]',
    description: 'A comprehensive set of tools designed for [target audience].',
    items: [
      { icon: 'i-lucide-zap', title: 'Feature One', description: 'Brief description of this feature and its benefit to the user.' },
      { icon: 'i-lucide-shield-check', title: 'Feature Two', description: 'Brief description of this feature and its benefit to the user.' },
      { icon: 'i-lucide-bar-chart-3', title: 'Feature Three', description: 'Brief description of this feature and its benefit to the user.' },
      { icon: 'i-lucide-users', title: 'Feature Four', description: 'Brief description of this feature and its benefit to the user.' },
      { icon: 'i-lucide-smartphone', title: 'Feature Five', description: 'Brief description of this feature and its benefit to the user.' },
      { icon: 'i-lucide-globe', title: 'Feature Six', description: 'Brief description of this feature and its benefit to the user.' }
    ]
  },

  // ──────────────────────────────────────────────
  // TESTIMONIALS — Social proof
  // ──────────────────────────────────────────────
  testimonials: {
    headline: 'Testimonials',
    title: 'Trusted by teams worldwide',
    items: [
      { name: 'Jane Doe', role: 'CEO at Company', avatar: 'https://i.pravatar.cc/128?img=1', quote: 'This product transformed how we handle [process]. Highly recommended.' },
      { name: 'John Smith', role: 'CTO at Startup', avatar: 'https://i.pravatar.cc/128?img=2', quote: 'We reduced our [metric] by 40% in the first month of using this platform.' },
      { name: 'Alice Johnson', role: 'Product Manager', avatar: 'https://i.pravatar.cc/128?img=3', quote: 'The best tool we have adopted this year. Simple, powerful, and reliable.' }
    ]
  },

  // ──────────────────────────────────────────────
  // APP DOWNLOAD — Mobile app store links
  // Set URL to empty string '' to hide that badge
  // ──────────────────────────────────────────────
  appDownload: {
    headline: 'Mobile app',
    title: 'Take it everywhere',
    description: 'Access your workspace on the go. Available on iOS and Android.',
    appStoreUrl: 'https://apps.apple.com/app/your-app-id',
    googlePlayUrl: 'https://play.google.com/store/apps/details?id=your.app.id'
  },

  // ──────────────────────────────────────────────
  // FAQ — Common questions
  // ──────────────────────────────────────────────
  faq: {
    headline: 'FAQ',
    title: 'Frequently asked questions',
    items: [
      { label: 'What is AppName?', content: 'Replace with your answer describing what the application does.' },
      { label: 'How much does it cost?', content: 'Replace with your pricing information.' },
      { label: 'Is there a free trial?', content: 'Replace with your trial policy.' },
      { label: 'How do I get started?', content: 'Replace with onboarding steps.' }
    ]
  },

  // ──────────────────────────────────────────────
  // FINAL CTA — Convert visitors
  // ──────────────────────────────────────────────
  finalCta: {
    title: 'Ready to get started?',
    description: 'Join thousands of users who already trust AppName. Create your free account today.',
    primaryCta: { label: 'Create free account', to: '/auth/register' },
    secondaryCta: { label: 'Contact sales', to: 'mailto:sales@example.com', icon: 'i-lucide-mail' }
  },

  // ──────────────────────────────────────────────
  // FOOTER
  // ──────────────────────────────────────────────
  footer: {
    copyright: '\u00A9 2026 AppName. All rights reserved.',
    links: [
      { label: 'Privacy Policy', to: '/privacy' },
      { label: 'Terms of Service', to: '/terms' },
      { label: 'Contact', to: 'mailto:contact@example.com' }
    ],
    socialLinks: [
      { icon: 'i-simple-icons-x', to: 'https://x.com/yourapp', label: 'X (Twitter)' },
      { icon: 'i-simple-icons-github', to: 'https://github.com/yourapp', label: 'GitHub' },
      { icon: 'i-simple-icons-linkedin', to: 'https://linkedin.com/company/yourapp', label: 'LinkedIn' }
    ]
  },

  // ──────────────────────────────────────────────
  // SEO — Search engine & social sharing
  // ──────────────────────────────────────────────
  seo: {
    title: 'AppName - Your tagline',
    description: 'A short SEO description of your application.',
    ogImage: '/og-image.png'
  },

  // ──────────────────────────────────────────────
  // AUTH — Keycloak redirect URLs
  // ──────────────────────────────────────────────
  auth: {
    loginUrl: '/auth/login',
    registerUrl: '/auth/register',
    accountUrl: '/auth/account'
  }
}
