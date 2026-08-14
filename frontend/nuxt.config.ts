import tailwindcss from '@tailwindcss/vite'

/**
 * Cached on the server, revalidated by the browser.
 *
 * `max-age=0` is the point: it forces a conditional request on every visit, so a reload
 * always reaches us and gets whatever the server cache holds now. `s-maxage` keeps the
 * server-side `swr` window, and applies to a CDN if one is ever put in front.
 */
const BROWSER_MUST_REVALIDATE = (seconds: number) =>
  `public, max-age=0, must-revalidate, s-maxage=${seconds}, stale-while-revalidate`

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
    //
    // Only the listings, though. `swr` makes Nuxt serve the payload separately and emit a
    // <link rel=preload> for `<route>/_payload.json`, and it builds that href by encoding a
    // path that is already percent-encoded: `/requests/%DA%A9…` is asked for as
    // `/requests/%25DA%25A9…/_payload.json`. That matches no route, Nitro answers with the
    // HTML 404 page, parsing it as JSON throws, and the page hydrates with no data and
    // renders its own 404 -- so every detail page looked correct to curl and broken in a
    // browser. The listing paths are ASCII and unaffected; the slug routes are not, and the
    // slugs are Persian by design, so those two rules are gone.
    //
    // The explicit cache-control is the second half of the freshness story. Nuxt emits
    // `s-maxage=300, stale-while-revalidate` with no `max-age`, and a response with no
    // max-age and no expires lets a browser invent its own freshness lifetime from
    // last-modified. So even after the server cache is dropped, a reload could be answered
    // out of the visitor's own browser cache without ever asking us — the centre publishes,
    // refreshes, and sees nothing change. `max-age=0` makes the browser revalidate every
    // time; `s-maxage` still governs the server cache and any CDN put in front later.
    '/': { swr: 300, headers: { 'cache-control': BROWSER_MUST_REVALIDATE(300) } },
    '/requests': { swr: 120, headers: { 'cache-control': BROWSER_MUST_REVALIDATE(120) } },
    '/centers': { swr: 600, headers: { 'cache-control': BROWSER_MUST_REVALIDATE(600) } },
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
