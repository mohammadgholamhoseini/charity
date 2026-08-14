# یاری‌جو — Charity Platform

An information board for charity requests. Charity centres publish the needs they have
verified; visitors browse them and contact the centre directly. **There is no online
payment and no donation tracking** — the platform announces requests, nothing more.

```
charity/
├── backend/     Spring Boot 3 REST API (Java 21, MySQL/H2, Flyway)
└── frontend/    Nuxt 3 SSR app (Vue 3, Tailwind v4, TypeScript)
```

## Roles

| Role | How it is created | What it can do |
|---|---|---|
| `ADMIN` | seeded from `ADMIN_INITIAL_PASSWORD` | publish/reject requests, manage centres, categories, announcements, locations |
| `CENTER` | created by an admin only | publish requests in its allowed categories |

There is no public registration. A request goes `DRAFT` → `PENDING` → `PUBLISHED`, and
only an admin can publish it; rejecting requires a written reason.

## Requirements

Java 21, Node 22, Maven, Docker (optional).

## Running locally

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile uses an in-memory H2 database on port **8085**, builds the schema
from the same Flyway migrations as production, and seeds categories, Iran's provinces
and cities, and an `admin` / `admin123` account.

```bash
cd frontend && npm install
NUXT_API_ORIGIN=http://localhost:8085 npm run dev
```

The site runs at **http://localhost:3000**.

## Running with Docker

Two branches run side by side, each from its own prebuilt image. `compose.yaml` has no
`build:` sections; images are produced per branch by a script that uses a `git worktree`,
so neither branch has to be checked out to build it:

```bash
./docker-build.sh all      # or: dev | master
docker compose up -d
```

| Service | Branch | URL | Database |
|---|---|---|---|
| frontend-master | master | http://localhost | — |
| frontend-dev | development | http://localhost:8080 | — |
| backend-master | master | http://localhost:81 | `YARIJU` |
| backend-dev | development | http://localhost:8081 | `YARIJU_DEVELOPMENT` |

Each branch has its own MySQL schema in the shared `mysql` service, so resetting or
migrating development never touches master's data. Both are created and granted by
`docker/mysql/init/01-databases.sql` on a fresh volume.

Only the master deployment sets `NUXT_PUBLIC_INDEXABLE=true`; the dev site serves a
`robots.txt` that disallows everything so it cannot compete with production in search.

## Configuration

Set in `.env` at the repo root:

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | **Required.** At least 32 bytes; the app refuses to start without it. |
| `ADMIN_INITIAL_PASSWORD` | Creates the first admin. No admin is seeded if unset. |
| `APP_SITE_URL` | Public site URL — used for canonical URLs and channel links. |
| `APP_BASE_URL` | API URL — used only to build file URLs. |
| `CORS_ORIGINS` | Only for direct browser access to the API port; the site itself proxies through its own origin. |
| `MYSQL_*` | Database credentials. |
| `TELEGRAM_*`, `BALE_*` | Optional announcement channels; disabled by default. |

`APP_SITE_URL` and `APP_BASE_URL` are deliberately separate. They used to be one value
pointing at the API, which made every "view on site" link posted to the messaging
channels resolve to the backend port.

## API

Public endpoints are unauthenticated and cacheable:

| Method | Path | Notes |
|---|---|---|
| GET | `/api/public/requests` | faceted: `category`, `urgency`, `city`, `status`, `q`, `sort`, `page`, `size` — all combinable |
| GET | `/api/public/requests/{slug}` | 200 / 301 stale slug / 404 / 410 removed |
| GET | `/api/public/requests/by-code/{code}` | 301 to the canonical URL |
| GET | `/api/public/centers`, `/api/public/centers/{slug}` | |
| GET | `/api/public/categories`, `/api/public/cities`, `/api/public/provinces` | |
| GET | `/api/public/announcements` | at most one per placement |
| GET | `/api/public/sitemap/*` | feeds the frontend's sitemap |
| POST | `/api/auth/login` | 5 failed attempts locks the account for 15 minutes |
| — | `/api/center/**` | `ROLE_CENTER` |
| — | `/api/admin/**` | `ROLE_ADMIN` |

Swagger is off by default and enabled with `SPRINGDOC_ENABLED=true`.

## Database

Flyway owns the schema on every profile. To change it, add a migration in
`backend/src/main/resources/db/migration` and run the `local` profile — it validates
the entity model against the migrated schema and will fail if the two disagree.

An existing pre-Flyway database is baselined automatically on first run
(`FLYWAY_BASELINE_ON_MIGRATE`, default `true`); V1 is skipped and V2 onward applied.

## Branches

- `master` — production-ready
- `development` — active development

## License

MIT
