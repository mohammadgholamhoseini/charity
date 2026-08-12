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

There are **zero tests** in either project and no lint/typecheck step in CI. The
frontend has `npm run typecheck` and `npm run lint` available but nothing enforces
them. Do not go looking for a test command.

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
- New requests default to `PENDING`; an admin publishes them. Rejecting requires a note.
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
| `frontend-master` | master | 80 | Nuxt SSR on 3000 |
| `frontend-dev` | development | 8080 | Nuxt SSR on 3000, `robots.txt` disallows all |
| `backend-master` | master | 81 | MySQL |
| `backend-dev` | development | 8081 | H2 |
| `mysql` | — | 127.0.0.1:3307 | |

The frontend containers run Node, not nginx — SSR needs a runtime. Both Dockerfiles are
identical; what used to differ (which backend to proxy to) is now `NUXT_API_ORIGIN`, so
one image can serve either environment. `app/server/routes/api/[...path].ts` replaces
the old nginx `/api/` proxy block.

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
