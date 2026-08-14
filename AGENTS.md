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
looking for a test command. `npm run lint` works; **`npm run typecheck` does not** — it
aborts with "Cannot find matching tsconfig.json" because the repo has no root tsconfig
and `nuxt typecheck` looks for one before consulting `.nuxt/`. Use `npm run build` to
find breakage instead.

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
- **A request has no city, deadline, contact details or beneficiary name.** The city and
  phone belong to the centre and are read from there — `?city=` and `?province=` filter
  through `request → center → city`, which is where `requests.city_id` was backfilled from
  in V2 anyway. The beneficiary is never named: the bot used to print
  `details.beneficiaryName` into a public channel while the site and the privacy page both
  promised it was never published.
- Deletion is **soft** (`deleted_at`), so a removed request answers **410**, not 404.
- A request's slug is frozen once published; changing it records the old one in
  `request_slug_history` so the old URL 301s.
- Public filtering goes through `RequestSpecifications`. Every facet composes. Do not
  add another bespoke query method — that is what the previous if/else ladder was.
- Amounts are required and public; there is deliberately no "amount collected".

## Frontend

Nuxt 3 (SSR), Vue 3 `<script setup>`, Tailwind v4, TypeScript, `@vueuse/nuxt`.
RTL Persian, **light theme only**. `srcDir` is `app/`, so pages, components and the
Nitro `server/` directory all live under `frontend/app/`.

Tailwind v4 is wired through `@tailwindcss/vite`, **not** `@nuxtjs/tailwindcss` (that
module is for v3 and expects a `tailwind.config.js`). All design tokens are in the
`@theme` block of `app/assets/css/main.css`.

Things that are easy to break:
- **Vazirmatn is self-hosted** in `public/fonts/`. Do not switch it to Google Fonts —
  that origin is unreliable from Iran and sits in the critical path of the LCP element.
- **Route params are percent-encoded.** Decode exactly once via `useRouteSlug()`, and
  do not encode again in `app/api/endpoints.ts` — the URL layer does that.
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
| `frontend-master` | master | 80 | **nginx on container port 80** — master has not been migrated to Nuxt yet |
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

The two frontends publish **different container ports** because they are different servers:
master's nginx listens on 80, development's Nuxt on 3000. Mapping master's host 80 to
container 3000 silently serves nothing at all. Fix this when master gets the Nuxt app.

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

## Git

- `master` — production-ready
- `development` — active branch
- Remote: `github.com/mohammadgholamhoseini/charity.git`

## Known issues

`ISSUES.md` is a full audit. Most critical and high items are now fixed, but note:
**the Bale bot token that was committed to `application.yml` is still live** — it has
been removed from the source but that does not invalidate it. It needs revoking in
BotFather, and `JWT_SECRET` should be rotated for the same reason.
