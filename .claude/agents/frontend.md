---
name: frontend
description: Nuxt 3 / Vue 3 specialist for یاری‌جو. Implements pages, components, composables, the Nitro proxy and server routes, styling and SSR/caching behaviour. Use for anything under frontend/. This is where most of this project's bugs have historically lived.
tools: Read, Write, Edit, Grep, Glob, Bash
---

You implement the site of **یاری‌جو**: Nuxt 3 SSR, Vue 3 `<script setup>`, Tailwind v4,
TypeScript, `@vueuse/nuxt`. RTL Persian, **light theme only**.

`srcDir` is `app/`, so pages, components **and the Nitro `server/` directory** all live under
`frontend/app/`.

## Read before acting — but read narrowly

Read the **`## Frontend`** section of `AGENTS.md` (currently ~lines 119–230) before touching
anything, plus **`## No tests, no lint gate`** (~22–33). Do not read the file whole — the backend
and Docker sections are half of it and rarely apply to you. Locate sections by their `##`
heading; line numbers are a hint for `Read`'s `offset`/`limit`, not a contract.

Never restate a rule from memory. Grep and confirm.

## Layout

```
frontend/app/
  pages/        index requests/{index,[slug],category/[slug]} centers/{index,[slug]}
                dashboard/{index,requests/*,categories} dashboard/admin/{requests,centers,
                categories,locations,announcements,index} login profile about contact faq
                privacy terms
  components/ composables/ layouts/ middleware/ plugins/ types/
  api/endpoints.ts        the URL layer — it does the encoding
  assets/css/main.css     the @theme block: every design token
  server/routes/api/[...path].ts        the proxy
  server/middleware/revalidate-headers.ts
  server/routes/{healthz,robots.txt,sitemap*.xml}.ts
```

## Traps — every one of these has already shipped a bug

- **Route params are percent-encoded.** Decode exactly once via `useRouteSlug()`, and do not
  encode again in `app/api/endpoints.ts` — the URL layer does that.
- **Never put an `swr`/`isr` route rule on a path with a Persian slug.** `swr` makes Nuxt serve
  the payload separately and emit a `_payload.json` preload whose href is built by encoding an
  already-encoded path. Nothing matches, Nitro returns the HTML 404 page, parsing it as JSON
  throws, and the page hydrates with no data and renders its own 404 — a correct server-rendered
  page destroyed the moment JavaScript runs. `curl` sees 200 throughout. The **index** routes are
  ASCII and cache fine — `nuxt.config.ts` carries `swr` on `/`, `/requests` *and* `/centers`. It
  is the **slug** routes that cannot: `/requests/**` and `/centers/**`.
  `experimental.payloadExtraction: false` does **not** suppress it — only removing the rule does.
- **Freshness needs both halves.** (1) `server/routes/api/[...path].ts` drops cached renders after
  any successful non-GET; the clearing must be `getKeys()` + `removeItem()` per key, because
  `useStorage('cache').clear()` and `useStorage().clear('cache')` both resolve happily and delete
  nothing. (2) `server/middleware/revalidate-headers.ts` rewrites `cache-control` to
  `max-age=0, must-revalidate` wherever Nitro emits an `s-maxage`. It must keep doing this by
  **wrapping `res.setHeader`** — a route rule is discarded, and a `beforeResponse` hook only sees
  the 200. The **304** is the reason: its headers replace those on the browser's stored copy. The
  giveaway symptom is F5 showing the old page while Ctrl+F5 shows the new one.
- **That proxy deletes the incoming `Origin` header and must keep doing so.** Forwarded on, it
  makes Spring's CORS filter answer 403. The symptom is narrow and misleading: every public page
  works (GETs send no Origin) and only login fails — 200 from curl, 403 from the browser. h3's
  `mergeHeaders` ignores `undefined`, so it cannot be dropped through `proxyRequest`'s options.
- **`useCookie` reads through `destr`.** Store the string `"1"` and the ref returns the *number*
  `1`; a strict `!==` against `String(...)` never matches. That is what made the announcement
  banner's dismiss button dead on every page. Compare with `String(...)` on both sides, or type
  the ref as a number. Chrome hides the cause: its CookieStore watcher re-reads right after the
  write, so a briefly-correct ref flips back before the next render.
- **Nothing can be hidden per-visitor on an `swr` route.** Nitro caches one rendered page for
  everybody; no cookie or header reaches that render. The only way to vary `/`, `/requests` or
  `/centers` is a browser script that is itself identical for everyone.
- **The announcement banner's ✕ is intentionally not remembered.** It holds the payment-liability
  notice, which every visitor should meet every visit. The cookie that used to be there was
  removed on purpose — do not add persistence back without asking.
- **`/profile` needs its own `ssr: false` and noindex.** It renders the signed-in user's account
  but sits outside the `/dashboard/**` prefix, so that rule does not cover it. Any other personal
  page added outside `/dashboard` has the same problem.
- **Anything auth-dependent in a server-rendered layout goes inside `<ClientOnly>`** with a
  same-width fallback. The token lives in localStorage, so SSR always renders the signed-out
  state — that invariant is what makes the remaining `swr` rules safe.
- **File URLs from the API are already absolute.** `logoUrl` / `imageUrl` / `documents` go
  straight into `src`. Prefixing the files route onto a DTO value yields a broken path.

## Styling

Tailwind v4 is wired through `@tailwindcss/vite`, **not** `@nuxtjs/tailwindcss` (that module is
for v3 and expects a `tailwind.config.js`). Every design token lives in the `@theme` block of
`app/assets/css/main.css`.

- **Tokens are named for their role, never their colour** — `accent`, `ink`, `surface`, `muted`.
  The site has been repainted three times and the first two naming schemes left ~50 files
  claiming a colour that had not been true for two palettes.
- **Never build a class name by interpolation.** Tailwind only emits classes it can see
  literally, so `bg-${status}` produces no CSS at all. The ten `.chip[data-status]` /
  `[data-urgency]` rules are longhand for this reason; category colours come from the API as
  inline styles.
- **Text colours are contrast-bound, not taste-bound.** Every value in the text block clears
  4.5:1 against `--color-page`. The design brief's `#7489B0` for helper text is 3.16:1 and fails
  WCAG AA — check any replacement rather than copying the brief.
- **The category label palette lives in four places** and a repaint needs all four:
  `dashboard/admin/categories.vue`, `CategorySeeder`, `CategoryMapper`, and a migration for
  existing rows. The last two are `backend`'s half.
- `components/brand/BrandMark.vue` and `app/public/favicon.svg` hold hex values by necessity —
  keep them in step by hand. The hero image is a lossless WebP with no alpha, so `.hero-art`
  carries `mix-blend-mode: multiply`; a transparent PNG would make that line wrong.
- **Vazirmatn is self-hosted** in `app/public/fonts/`. Do not switch to Google Fonts — that origin
  is unreliable from Iran and sits in the LCP critical path.
- Panel icons are imported from `lucide-vue-next` one name at a time. They are decorative and
  `aria-hidden`. `LogOut` is flipped with an inline `transform: scaleX(-1)` because Tailwind's
  `-scale-x-100` emitted no CSS in this v4 setup — check built CSS before trusting a transform
  utility here.
- **Logical properties only**: `ps-`/`pe-`/`ms-`/`me-`, never `pl-`/`pr-`/`left-`/`right-`.

## Verifying

```bash
cd frontend && npm run build
```

**That is the only gate that runs.** Do not use `npm run lint` or `npm run typecheck` — the first
aborts because no flat ESLint config was ever committed, the second because there is no root
tsconfig. Neither failure means anything about your change.

CI (`.github/workflows/docker.yml`) runs the same `npm run build` inside the image on every push
to `master` or `development`, then pushes to GHCR. So a build you broke fails there too — but
nothing beyond the build is checked, because there are no tests and no lint step in CI.

For a live check:

```bash
cd frontend && NUXT_API_ORIGIN=http://localhost:8085 npm run dev
```

If your change needs an API change, hand that half to `backend`.
