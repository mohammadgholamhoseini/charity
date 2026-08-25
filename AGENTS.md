# AGENTS.md — یاری‌جو (Charity Platform)

## What this is

An **information board** for charity requests. There is no online payment: visitors
browse requests and contact the registering centre directly. Two roles, `ADMIN` and
`CENTER`; there is **no public registration** — admins create centre accounts.

## Quick start

```bash
# Backend (H2 in-memory, port 8085)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Frontend (Nuxt SSR, port 3000)
cd frontend && npm install && NUXT_API_ORIGIN=http://localhost:8085 npm run dev
```

Local admin: `admin` / `admin123` (from `app.admin.initial-password`, which defaults to
that only in the `local` profile). Categories, provinces and cities are seeded.

## No tests, no lint gate

There are **zero tests** in either project and no lint/typecheck step in CI. Do not go
looking for a test command, and do not trust the two scripts that look like one:

- `npm run typecheck` aborts with "Cannot find matching tsconfig.json" — the repo has no
  root tsconfig and `nuxt typecheck` looks for one before consulting `.nuxt/`.
- `npm run lint` aborts with "ESLint couldn't find an eslint.config.js" — `@nuxt/eslint`
  is in devDependencies but no flat config was ever committed.

**`npm run build` is the only gate that runs.** Use it.

## Backend

Spring Boot 3.3.5, Java 21, Maven, Lombok, jjwt 0.12.6, Springdoc.

| Profile | DB | Port | Schema |
|---|---|---|---|
| default | MySQL | 8082 | Flyway, `ddl-auto: none` |
| `local` | H2 in-memory | 8085 | Flyway, `ddl-auto: validate` |

**Flyway owns the schema on both profiles.** The default profile uses `ddl-auto: none`
because it points at the populated production database, whose columns were created by
the old `ddl-auto: update` and can differ harmlessly from what Hibernate would emit.
The `local` profile builds from the same migrations and then *validates* the entities
against the result — so a mismatch between a migration and an entity fails a dev run
rather than a production start. If you change an entity, add a migration and run the
`local` profile; that is the check.

Migrations live in `backend/src/main/resources/db/migration` and are written in the
dialect subset shared by MySQL 8.4 and H2 in MySQL mode (no `ENGINE`/`CHARSET`
clauses, no `RENAME TABLE`, no `INSERT IGNORE`). Anything needing Persian-aware text
handling — slug generation in particular — is done in Java (`config/seed/`) instead,
because it cannot be expressed portably in SQL.

Key domain notes:
- The entity is `Request` (table `requests`), formerly `CharityCase`.
- **A centre publishes its own requests; there is no admin approval.** Creating with
  `submit: true` lands straight in `PUBLISHED`; `false` saves a `DRAFT`. `PENDING` and
  `REJECTED` are unreachable and kept only so pre-V9 rows still deserialise — the live
  workflow is whatever `RequestStatusPolicy.ALLOWED` says, nothing else. An admin can take
  a request down (`INACTIVE`, note required), put it back, or mark it `COMPLETED`.
- **A centre manages its own listing; an admin's takedown outranks it.** A centre may publish,
  complete, and withdraw its own requests through `POST /api/center/requests/{id}/status`. What it
  may not do is reverse a deactivation an admin made — `requests.deactivated_by` records which role
  took it down, and the centre paths refuse when it says `ADMIN`. Before V11 there was no such
  column, `INACTIVE → PUBLISHED` was simply allowed, and the publish path cleared `status_note` on
  the way, so a centre could undo a moderation decision and erase its stated reason with it. The
  column is set entering `INACTIVE` and cleared leaving it; leaving it set would lock a centre out
  of a request it withdrew itself.
- **`COMPLETED` is terminal, and the two roles differ only there.** `RequestStatusPolicy` holds two
  maps that are identical apart from one row: a centre can move a completed request nowhere at all,
  and an admin can move it only to `INACTIVE`. Nobody can reopen one — republishing a request whose
  need was already met puts a live listing back in front of donors for something already paid for.
  The admin's `COMPLETED → INACTIVE` is the deliberate exception and the *only* remedy in the
  product for a completed request that turns out to be fraudulent or plain wrong; without it the
  answer would be hand-written SQL. A centre also cannot **delete** a completed request
  (`isDeletableByCenter`), for the same reason it cannot edit one. An admin still can, and deletion
  is soft either way. Both panels mirror these maps client-side purely so the dialog never offers a
  move the server answers with 409 — the server is the authority, and the panels are checked
  against it, not trusted by it.
- **Anything the bot message template touches must be fetch-joined.** The announcement listener is
  `@Async` and `open-in-view` is off, so the entity reaches it detached: a plain `findById` gives it
  uninitialised proxies and the first association read throws `LazyInitializationException` into the
  catch-all in `AbstractBotMessagingService`, which logs and returns null. No HTTP call is attempted
  and `bale_posted` stays false. That is how Bale silently never worked at all. Load through
  `RequestRepository.findForMessaging`, and if you add a field to the template, add it to that
  query. It is JPQL rather than `@EntityGraph` deliberately — entity-graph paths are unchecked
  strings that fail at runtime, JPQL fails at startup.
- **An announcement gets one automatic attempt, and a manual one after that.** The event fires on
  create-with-publish and on the *first* transition to `PUBLISHED` — nowhere else — so a channel
  that was down at that moment stayed missing permanently. RQ-1017 is the worked example. The loop
  now lives in `RequestAnnouncementService`, shared by the `@Async` listener and the panel's
  «انتشار در کانال» button (`POST /api/{admin,center}/requests/{id}/announce`). Three things about
  it are load-bearing. It skips any channel where `MessagingService.alreadyPosted` is true, so a
  retry never duplicates into the channel that already worked. The manual path is **synchronous**,
  unlike the listener — that button is a rare click with nothing behind it, and running it inline
  is what lets the response carry the true state and a failure reach the user as an error instead
  of silence. And neither the service nor the listener is `@Transactional`: the bot HTTP calls must
  not run inside a transaction, so every database touch goes through `RequestService`, which opens
  its own. `RequestSummary.announced` is what the panel keys the button on; it is computed by
  asking the enabled channels, not by reading `bale_posted`, so a disabled Telegram does not leave
  every row looking forever unannounced.
- **A request has no city, deadline or contact details of its own.** The city and
  phone belong to the centre and are read from there — `?city=` and `?province=` filter
  through `request → center → city`, which is where `requests.city_id` was backfilled from
  in V2 anyway.
- **Naming the beneficiary is the centre's call, not the platform's.** There is no field for a
  beneficiary's name, phone or address, and deliberately never will be — the only place such a
  detail can appear is inside the free-text description the centre writes. In August 2026 the
  project owner made publishing it **optional**: the centre decides, the centre carries the
  responsibility, and the centre must hold the beneficiary's **written consent** first.
  `frontend/app/pages/privacy.vue` states exactly that, and `RequestForm.vue` reminds the centre
  beside the description field. This **reverses** the older absolute rule, so do not "fix" it
  back. The bot printing `details.beneficiaryName` into a public channel was a bug because the
  privacy page promised the opposite at the time — not because a centre may never name anyone.
  What is still forbidden is the platform *asking for* or *storing* beneficiary identity in its
  own columns, and any feature that would surface it beyond the text the centre wrote.
- Deletion is **soft** (`deleted_at`), so a removed request answers **410**, not 404. A centre
  cannot delete a `COMPLETED` request at all; an admin can.
- A request's slug is frozen once published; changing it records the old one in
  `request_slug_history` so the old URL 301s.
- Public filtering goes through `RequestSpecifications`. Every facet composes. Do not
  add another bespoke query method — that is what the previous if/else ladder was.
- Amounts are required and public; there is deliberately no "amount collected".
- **The login lock is not only about logins.** Five failed attempts lock an account for 15
  minutes through `users.failed_attempts` / `locked_until`, and a wrong `currentPassword` at
  `PUT /api/center/me` or `PUT /api/admin/me` counts against that same lock — an admin can
  lock itself out of the panel from the profile form, and `JwtAuthenticationFilter` re-checks
  the lock per request, so the live session dies at once rather than at next login.
  `LoginAttemptService` owns the counter. The increment is a `@Modifying` statement on
  `UserRepository` and the lock is a *second* statement carrying the threshold in its `WHERE`;
  never read the count back to decide, because that is the lost update that let 50 concurrent
  guesses all read 0, all write 1, and never trip the lock at all.
- **Count a failed attempt with no transaction open.** The counter used to be `REQUIRES_NEW` so
  it survived the caller's rollback — but nested inside `updateOwnProfile`'s transaction it
  asked Hikari for a second connection while holding the first, and 20 concurrent attempts
  deadlocked the pool for 30 seconds with one valid token. That is why `PasswordChangeGuard`
  rejects by throwing `IncorrectCurrentPasswordException` and `GlobalExceptionHandler` does the
  counting, after the caller's transaction has unwound and released its connection.

## Frontend

Nuxt 3 (SSR), Vue 3 `<script setup>`, Tailwind v4, TypeScript, `@vueuse/nuxt`.
RTL Persian, **light theme only**. `srcDir` is `app/`, so pages, components and the
Nitro `server/` directory all live under `frontend/app/`.

Tailwind v4 is wired through `@tailwindcss/vite`, **not** `@nuxtjs/tailwindcss` (that
module is for v3 and expects a `tailwind.config.js`). All design tokens are in the
`@theme` block of `app/assets/css/main.css`.

**Tokens are named for their role, never for their colour.** `accent`, `ink`, `surface`,
`muted` — not `brick`, `gold`, `cream`. The site has been repainted three times (coral,
then brick-and-gold, now the lapis-and-lacquer of `redesign.md` 7c) and the first two
naming schemes left ~50 files claiming a colour that had not been true for two palettes.
A repaint should be an edit to the `@theme` block and nothing else. Two things stop that
being literally true, and both are unavoidable:

- `BrandMark.vue` and `public/favicon.svg` hold hex values, because an SVG `stroke` takes
  no Tailwind class and `var(--color-…)` breaks wherever the mark is inlined without the
  stylesheet. Keep the two in step by hand.
- The ten `.chip[data-status]` / `[data-urgency]` rules are written out longhand. Tailwind
  only emits classes it can see literally, so `bg-${status}` produces no CSS at all.

**Text colours are contrast-bound, not taste-bound.** `redesign.md` specifies `#7489B0`
for helper text, which is 3.16:1 on the page ground and fails WCAG AA — and `muted` is
the most-used text colour on the site. Every value in the text block clears 4.5:1 against
`--color-page`; check any replacement before shipping it rather than copying the brief.

Things that are easy to break:
- **Vazirmatn is self-hosted** in `public/fonts/`. Do not switch it to Google Fonts —
  that origin is unreliable from Iran and sits in the critical path of the LCP element.
- **Route params are percent-encoded.** Decode exactly once via `useRouteSlug()`, and
  do not encode again in `app/api/endpoints.ts` — the URL layer does that.
- **Freshness on the cached pages takes two pieces, and both are load-bearing.**
  1. `app/server/routes/api/[...path].ts` drops the cached renders after any successful
     non-GET, because every panel write passes through it. Clearing has to be `getKeys()` +
     `removeItem()` per key — `useStorage('cache').clear()` and `useStorage().clear('cache')`
     both resolve happily and delete nothing.
  2. `app/server/middleware/revalidate-headers.ts` rewrites `cache-control` to
     `max-age=0, must-revalidate` wherever Nitro emits an `s-maxage`. Nitro sends no
     `max-age`, and a response with neither `max-age` nor `expires` lets a browser invent
     its own freshness from `last-modified` and answer a reload from its own cache. The
     giveaway is F5 showing the old page while Ctrl+F5 shows the new one.

     It has to be done by wrapping `res.setHeader`, and the reason is the **304**. A route
     rule is discarded (Nitro's cache layer assigns `cache-control` after route rules run),
     and a `beforeResponse` hook only ever sees the 200 — h3's `handleCacheHeaders` writes
     `public, max-age=<n>, s-maxage=<n>` and calls `res.end()` itself. A 304's headers
     *replace* those on the browser's stored copy, so that one response was telling every
     browser "do not ask again for five minutes" the moment it revalidated successfully.
- **Never put a `swr`/`isr` route rule on a path with a Persian slug.** `swr` makes Nuxt
  serve the payload separately and emit `<link rel="preload" href="<route>/_payload.json">`,
  and it builds that href by encoding a path that is already percent-encoded:
  `/requests/%DA%A9…` is requested as `/requests/%25DA%25A9…/_payload.json`. Nothing matches,
  Nitro returns the HTML 404 page, parsing it as JSON throws, and the page hydrates with no
  data and renders its own 404 — a correct server-rendered page destroyed the moment
  JavaScript runs. `curl` sees 200 and the right content throughout. `/` and `/requests` are
  ASCII and cache fine; `/requests/**` and `/centers/**` cannot. `experimental.payloadExtraction:
  false` does *not* suppress it — only removing the route rule does.
- **`/profile` needs its own `ssr: false` and noindex.** It renders the signed-in user's own
  account for both roles, but it sits outside the `/dashboard/**` prefix, so that rule does not
  cover it. Any other personal page added outside `/dashboard` has the same problem.
- **File URLs from the API are already absolute.** `AppUrls.fileUrl` returns
  `<apiBaseUrl>/api/public/files/<name>`, so a DTO's `logoUrl` / `imageUrl` / `documents` go
  straight into `src`. The entity stores a bare filename; the DTO never exposes it. Prefixing the
  files route onto a DTO value yields a broken path.
- **Anything auth-dependent in a server-rendered layout must be inside `<ClientOnly>`**
  with a same-width fallback. The token lives in localStorage, so SSR always renders
  the signed-out state; this invariant is what makes the `swr` route rules safe.
- **`app/server/routes/api/[...path].ts` deletes the incoming `Origin` header** before
  proxying, and must keep doing so. The browser attaches Origin to every non-GET
  request, same-origin included; forwarded on, it makes Spring's CORS filter reject the
  call with 403 unless the site's own URL is in `cors.allowed-origins`. The symptom is
  narrow and misleading — public pages work (GETs send no Origin) and only login fails,
  200 from curl and 403 from the browser. Note that h3's `mergeHeaders` ignores
  `undefined`, so the header cannot be dropped through `proxyRequest`'s options.
- Status and urgency chip colours are static CSS keyed on `data-` attributes. Category
  colours come from the API as inline styles. Never build a class name by interpolation.
- **The category label palette lives in four places and a repaint needs all four.** The
  admin picks a background/foreground pair per category and it is stored *on the row*, so
  changing CSS does nothing: `dashboard/admin/categories.vue` (the swatch picker),
  `CategorySeeder` (fresh installs), `CategoryMapper` (the fallback pair), and a migration
  for the rows that already exist — `V10` is the worked example. Guard that migration on
  the old colour, not on the slug alone: `CategorySeeder` promises never to overwrite an
  admin's own choice, and an unconditional `UPDATE` breaks that promise silently.
- The hero image is the only photography on the site. It is a lossless WebP with **no
  alpha channel**, so `.hero-art` carries `mix-blend-mode: multiply` to dissolve its white
  background into the page. Replace it with a transparent PNG and that line should go.
- **`useCookie` reads through `destr`, so what you wrote is not what you get back.** Store the
  string `"1"` and the ref returns the **number** `1`; a strict `!==` against a `String(...)`
  never matches. That is what made the announcement banner's dismiss button dead — the click
  wrote the cookie correctly every time and nothing happened, on that page and every page after.
  Compare with `String(...)` on both sides, or type the ref as the number. Chrome hides the cause
  further: its CookieStore watcher re-reads the cookie right after the write, so a ref that was
  briefly correct flips back before the next render.
- **Nothing can be hidden per-visitor on an `swr` route by reading the request.** Nitro caches one
  rendered page for everybody and no cookie or header reaches that render, so `/`, `/requests` and
  `/centers` ship identical HTML to all comers. The only way to vary them is a script that runs in
  the browser and is itself the same for everyone. Do not reach for a route rule, and do not
  assume a cookie the server *can* read on `/requests/<slug>` will also work on `/requests`.
- **The announcement banner's ✕ is intentionally not remembered.** It holds the payment-liability
  notice, which every visitor should meet on every visit, so the state is a plain `ref` and a
  reload brings it back. That also means the server and client render the same thing and nothing
  moves at hydration. Do not "fix" it by adding persistence without asking — the cookie that used
  to be there was removed on purpose.
- **Panel icons come from `lucide-vue-next`**, imported one name at a time so the bundle carries
  only what is used (ten icons ≈ 7 kB). They are decorative — every one sits beside its own label
  and is `aria-hidden`. `LogOut` is mirrored with `transform: scaleX(-1)` because the panel is
  RTL. Note that Tailwind's `-scale-x-100` emitted no CSS in this v4 setup, so the flip is an
  inline style on purpose; check the built CSS before trusting a transform utility here.
- Use logical properties (`ps-`/`pe-`/`ms-`/`me-`), never `pl-`/`pr-`/`left-`/`right-`.

## Docker

`compose.yaml` is **image-based** (no `build:`), so `docker compose build` does nothing.
Images are built per branch by `./docker-build.sh`, which uses a `git worktree` so each
branch builds without switching or pushing.

```bash
./docker-build.sh all           # or: dev | master
docker compose up -d
```

| Service | Branch | Host port | Notes |
|---|---|---|---|
| `frontend-master` | master | 80 | Nuxt SSR on 3000, the only deployment that may be indexed |
| `frontend-dev` | development | 8080 | Nuxt SSR on 3000, `robots.txt` disallows all |
| `backend-master` | master | 81 | MySQL schema `YARIJU` |
| `backend-dev` | development | 8081 | MySQL schema `YARIJU_DEVELOPMENT` |
| `mysql` | — | 127.0.0.1:3307 | both schemas live in this one server |

**Each branch owns a MySQL schema**, so development can be reset without touching master's
data. `backend-dev` used to run the in-memory H2 `local` profile and lost everything on
restart; it now runs the same `default` profile as production against
`YARIJU_DEVELOPMENT`, with two deliberate differences: `ddl-auto: validate` (the schema is
Flyway-built from V1, so the entities can be checked against it — this is the drift guard
production's `none` gives up) and `baseline-on-migrate: false` (nothing to baseline, and a
non-empty schema with no history should fail loudly rather than silently skip V1).

`MYSQL_DATABASE` can only ever create one schema, so `docker/mysql/init/01-databases.sql`
creates the second and grants `charity_user` on both. It runs only on a fresh `mysql_data`
volume. When granting, escape the underscore — MySQL reads the database name in `GRANT` as
a LIKE pattern, where a bare `_` is a wildcard.

Both frontends now publish container port **3000**. Master used to publish 80 because it
still shipped the old static nginx image; that ended when the Nuxt rewrite was merged, in
the same commit that changed the mapping. A host-80-to-container-80 mapping against the
Node image serves nothing at all and fails silently, so check this line first if master
answers connection-refused.

The frontend containers run Node, not nginx — SSR needs a runtime. Both Dockerfiles are
identical; what used to differ (which backend to proxy to) is now `NUXT_API_ORIGIN`, so
one image can serve either environment. `app/server/routes/api/[...path].ts` replaces
the old nginx `/api/` proxy block.

**Building from a connection that cannot reach Docker Hub reliably.** Reaching
`registry-1.docker.io` from here fails intermittently with `TLS handshake timeout` — not
a hard block, just a link that drops most handshakes. So `docker-build.sh` pre-pulls the
three base images in a retry loop before building (`node:22-alpine` needed 6 attempts,
`maven:...` needed 6), then builds with `--pull=false` so BuildKit resolves `FROM` from
the local cache and never touches the registry again. Once cached, builds are offline.
Raise `PULL_RETRIES` if the link is worse than usual. Do not put a per-attempt `timeout`
around the pull — the Maven image is 585 MB and a short cap kills it mid-download every
time, which looks exactly like a hard failure.

Do not add `apk add` to the frontend Dockerfiles. Alpine's CDN *is* hard-blocked here, so
any `apk` call fails the build outright. Neither package it used to install was needed:
busybox already provides `wget` for the healthcheck, and Node resolves `TZ=Asia/Tehran`
through its bundled ICU — verified in the running container, which has no
`/usr/share/zoneinfo` at all and still reports `GMT+0330`.

`NUXT_PUBLIC_INDEXABLE` must stay `"false"` anywhere that is not production, otherwise
two identical sites compete for the same queries.

## Production (the VPS)

`compose.prod.yaml` and `nginx/default.conf` are in the repo as **mirrors of what runs at
`/opt/yariju/charity`, not as the source of truth**. The `deploy` job copies no files — it
SSHes in, pulls the images from GHCR and runs `up -d backend frontend nginx` against the
copies already on the server. An edit here reaches production only when the same edit is
made there by hand. `.env` is never committed; build it from `.env.prod.example`.

The topology differs from `compose.yaml` in the one way that matters:

```
browser ──https──> nginx ──┬── /api/ ──> backend:8082
                           └── /     ──> frontend:3000
```

Backend and frontend only `expose`; nginx is the sole thing on a host port. **`/api/` does
not pass through Nuxt here.** The local claim that the frontend's own `/api` route strips
`Origin` is true of `compose.yaml` and false of production.

- **`CORS_ORIGINS` must name the site origin exactly, scheme included.** nginx redirects
  http to https, so the browser always sends `Origin: https://yariju.com`, and nginx hands
  it to Spring untouched. `http://yariju.com` matched nothing and answered **403 to every
  `POST /api/auth/login`** — for weeks, on production only, while both local branches were
  fine. Diagnosing it is unusually slow because CORS rejects *before* authentication runs:
  `users.failed_attempts` stays at `0`, no account is locked, and nothing in the logs
  implicates the credentials. **A 403 on login is a CORS symptom, not a password one** —
  `/api/auth/**` is `permitAll` and CSRF is disabled, so nothing else in the chain can
  produce it.
- **`APP_BASE_URL` needs the same `https://`.** It builds the absolute URLs for uploaded
  files, so an `http://` value gets every image blocked as mixed content — pictures vanish
  while the pages themselves look fine.
- **MySQL publishes `127.0.0.1:3307` only**, and must stay that way. Reach it from a GUI
  client through an SSH tunnel to the VPS, then `127.0.0.1:3307`; set
  `serverTimezone=Asia/Tehran` in the client or every timestamp reads shifted. Before this
  binding existed, `docker compose -f compose.prod.yaml port mysql 3306` answered
  `invalid IP:0`.
- **The deploy names only `backend`, `frontend` and `nginx`.** `mysql` and `certbot` are
  never recreated by it, so a push to `master` cannot disturb the database or the
  certificates — and cannot fix them either.
- **TLS renews itself now, and did not used to.** The certificate was first issued with
  `authenticator = manual` and `pref_challs = dns-01`, which cannot run unattended —
  `certbot renew` refuses with *"An authentication script must be provided with
  `--manual-auth-hook` when using the manual plugin non-interactively"*. There was no cron
  either, so the site was on course to go dark the day the certificate expired. The
  renewal config now says `authenticator = webroot` with `webroot_path = /var/www/certbot`,
  which is the path nginx already serves `/.well-known/acme-challenge/` from, and
  `/etc/cron.d/certbot-renew` runs `renew-cert.sh` twice a day. dns-01 bought nothing here:
  it is only required for wildcards, and this certificate names `yariju.com` and
  `www.yariju.com` explicitly. **After changing anything about TLS, prove it with
  `docker compose -f compose.prod.yaml run --rm certbot renew --dry-run`** — it exercises
  the whole challenge path against the staging API without touching the live certificate
  or the issuance quota, and it is the only thing that distinguishes a renewal that works
  from one that merely looks configured.
- **nginx's `client_max_body_size` on `/api/` must stay above the backend's
  `MAX_REQUEST_SIZE`** (default 40MB); it is 45M. Whichever limit is tighter is the one
  that answers, and Spring answers better: `GlobalExceptionHandler` turns
  `MaxUploadSizeExceededException` into a 413 carrying the app's Persian error body, where
  nginx emits a raw HTML error page the frontend cannot parse. It was 20M, which silently
  swallowed multi-file uploads — `POST /api/center/requests/{id}/documents` takes an
  unbounded `List<MultipartFile>` and each file may be 10MB, so three ordinary documents
  were refused with no usable message.

## Git

- `master` — production-ready
- `development` — active branch
- Remote: `github.com/mohammadgholamhoseini/charity.git`

**`master` is permanently "ahead" of `development`, and the number means nothing.**
`git rev-list --left-right --count origin/master...origin/development` read 19 on master's
side in August 2026, and it grows by one with every release. Those 19 were: five
`Merge branch 'development'` commits, which exist only on master by construction; thirteen
`Create/Update ssh-test.yml` commits made directly on master through the GitHub web UI for
a throwaway workflow that `b023b69` then deleted outright; and two `Update docker.yml`
whose content reached `development` by other routes. Net contribution to the tree: zero.

**The count is not the check. The diff is:**

```bash
git diff origin/master origin/development --stat
```

Empty output means the trees agree and there is nothing to back-merge, however alarming
the count looks. This has been verified once — do not re-investigate the backlog each
time. Merging `master` into `development` to "catch up" on an empty diff only adds another
empty merge commit. Back-merge only when that diff actually shows something.

Those thirteen `ssh-test.yml` commits are also the cautionary tale: they were pushed
straight to `master`, and **a push to `master` is a production deployment** (see
`## Production (the VPS)`). Iterate on a workflow from `development` with
`workflow_dispatch`, which builds without deploying.


## Known issues

`ISSUES.md` is a full audit. Most critical and high items are now fixed, but note:
**the Bale bot token that was committed to `application.yml` is still live** — it has
been removed from the source but that does not invalidate it. It needs revoking in
BotFather, and `JWT_SECRET` should be rotated for the same reason.
