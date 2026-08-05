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

```bash
# All services up on different ports
docker compose up --build
```

| Service | Container | Port | DB |
|---------|-----------|------|----|
| `backend-master` | master branch | 81 | MySQL |
| `backend-dev` | dev branch | 8081 | H2 |
| `frontend-master` | master branch (nginx) | 80 | — |
| `frontend-dev` | dev branch (nginx) | 8080 | — |
| `mysql` | — | 3307 | MySQL |

Images are tagged by service name: `charity-backend:master`, `charity-backend:dev`, `charity-frontend:master`, `charity-frontend:dev`.

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
