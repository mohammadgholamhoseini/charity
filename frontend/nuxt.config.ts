import tailwindcss from '@tailwindcss/vite'

export default defineNuxtConfig({
  compatibilityDate: '2026-08-01',
  srcDir: 'app/',
  devtools: { enabled: true },

  modules: ['@vueuse/nuxt'],

  css: ['~/assets/css/main.css'],

  // Tailwind v4 ships as a Vite plugin. The @nuxtjs/tailwindcss module is for v3 and
  // expects a tailwind.config.js, which this project does not have -- the theme lives
  // in an @theme block in main.css.
  vite: {
    plugins: [tailwindcss()],
  },

  typescript: { strict: true, typeCheck: false },

  experimental: {
    /**
     * Off because the slugs are Persian.
     *
     * Prerendering the five static pages switches payload extraction on for the whole app,
     * and the client then asks for `<route>/_payload.json` on every server-rendered page too.
     * It builds that URL by encoding a path that is already percent-encoded, so
     * `/requests/%DA%A9…` becomes `/requests/%25DA%25A9…/_payload.json`, which matches no
     * route. Nitro answers with the HTML 404 page, parsing it as JSON throws, the payload is
     * lost, and hydration re-runs the page with no data — so every request and centre detail
     * page rendered a 404 in the browser while curl saw the correct page. The extraction only
     * ever saved a request on five static ASCII pages.
     */
    payloadExtraction: false,
  },

  runtimeConfig: {
    // Server-only. SSR fetches the API directly rather than looping back through our
    // own proxy. Overridden per environment by NUXT_API_ORIGIN.
    apiOrigin: 'http://localhost:8085',
    public: {
      // The browser always talks to the same origin; server/routes/api forwards it.
      apiBase: '/api',
      siteUrl: 'http://localhost:3000',
      siteName: 'یاری‌جو',
      siteTagline: 'شبکه مراکز خیریه',
      supportPhone: '۰۲۱-۱۲۳۴۵۶۷۸',
      // Only the production deployment may be indexed; the dev image must not be.
      indexable: false,
    },
  },

  app: {
    head: {
      htmlAttrs: { lang: 'fa-IR', dir: 'rtl' },
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'theme-color', content: '#B24A2E' },
        { name: 'format-detection', content: 'telephone=no' },
      ],
      link: [
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        // The hero heading is the LCP element, so the font is preloaded. It is
        // self-hosted rather than pulled from Google Fonts: that origin is unreliable
        // from Iran, and this is a Persian-audience site.
        {
          rel: 'preload',
          as: 'font',
          type: 'font/woff2',
          crossorigin: '',
          href: '/fonts/vazirmatn-variable.woff2',
        },
      ],
    },
  },

  routeRules: {
    // Public pages are identical for every visitor -- the panel is client-rendered and
    // the header's auth state is client-only -- so the rendered HTML is safe to cache.
    '/': { swr: 300 },
    '/requests': { swr: 120 },
    '/requests/**': { swr: 300 },
    '/centers': { swr: 600 },
    '/centers/**': { swr: 600 },
    '/about': { prerender: true },
    '/contact': { prerender: true },
    '/faq': { prerender: true },
    '/terms': { prerender: true },
    '/privacy': { prerender: true },

    // The panel holds per-user data and must never be server-rendered or indexed.
    '/dashboard/**': {
      ssr: false,
      headers: { 'X-Robots-Tag': 'noindex, nofollow' },
    },
    '/login': { headers: { 'X-Robots-Tag': 'noindex, follow' } },

    // Legacy URLs from the previous SPA.
    '/cases': { redirect: { to: '/requests', statusCode: 301 } },
    '/profile': { redirect: { to: '/dashboard/settings', statusCode: 301 } },
  },

  nitro: {
    compressPublicAssets: { gzip: true, brotli: true },
  },
})
