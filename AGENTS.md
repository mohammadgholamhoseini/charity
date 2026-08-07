# AGENTS.md — Charity Platform

## Quick start

```bash
# Backend (local H2, no MySQL needed, port 8085)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Frontend (port 5173, proxies /api to 8082)
cd frontend && npm install && npm run dev
```

Admin login: `admin` / `admin123`.  
Seed data: 8 categories + 1 default notice. **Provinces/Cities are NOT seeded** — create via Admin panel.

## No tests, no lint, no typecheck

There are **zero tests** in either backend or frontend. No lint/format/typecheck scripts exist. The tsconfig.json in frontend is dormant (all source files are `.js`). Do not look for or run test/lint commands.

## Backend profiles

| Profile | DB | Port | ddl-auto |
|---------|----|------|----------|
| default | MySQL (via `SPRING_DATASOURCE_URL` env) | 8082 | update |
| `local` | H2 in-memory | 8085 | create-drop |

Backend: Spring Boot 3.3.5, Java 21, Maven, Lombok (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor @RequiredArgsConstructor`), jjwt 0.12.6, Springdoc OpenAPI at `/swagger-ui/` + `/v3/api-docs/`.

## Docker

`compose.yaml` is **image-based** (no `build:`), so `docker compose build` does nothing. Images are built **per branch** via `./docker-build.sh`, which uses a `git worktree` so each branch builds its own image at the same time without pushing or switching branches.

```bash
# Build images, then start services
./docker-build.sh all           # or: ./docker-build.sh dev|master
docker compose up -d
```

`docker-build.sh` auto-detects the Docker binary (`docker`, or Windows `docker.exe` when run from WSL, converting paths with `wslpath`). Tags: `charity-backend:dev|master`, `charity-frontend:dev|master`.

| Service | Branch | Port | DB |
|---------|--------|------|----|
| `backend-master` | master | 81 | MySQL |
| `backend-dev` | development | 8081 | H2 |
| `frontend-master` | master (nginx) | 80 | — |
| `frontend-dev` | development (nginx) | 8080 | — |
| `mysql` | — | 3307 | MySQL |

Profiles are selected via `SPRING_PROFILES_ACTIVE` (`default` for master/MySQL, `local` for dev/H2) — the misspelt `SPRING_PROFILE` in older compose did not activate the profile.

To publish local `development` changes: commit them, then `./docker-build.sh dev` and `docker compose up -d frontend-dev backend-dev`. The `master` image is only rebuilt when the `master` branch actually changes.

## Frontend

Vue 3 (`<script setup>`), Vite 6, Tailwind v4 (postcss), Pinia, Vue Router, Axios, Lucide icons, VueUse.  
Dark mode forced on (`document.documentElement.classList.add('dark')`), RTL Persian.  
Dev proxy: `/api` → `http://localhost:8082`.

## Architecture reference

Read `ARCHITECTURE.md` (at `../ARCHITECTURE.md` from the repo root) for the full business rules, domain model, endpoint list, and `details` JSON column pattern.

## Git

- `master` — production-ready
- `development` — active branch
- Remote: `github.com/mohammadgholamhoseini/charity.git`
