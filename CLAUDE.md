# CLAUDE.md

## Read `AGENTS.md` first

**`AGENTS.md` is the source of truth for this project's domain rules.** It is ~375 lines and
nearly every bullet records a bug that already shipped. Nothing in it is repeated here, on
purpose — two copies of a rule become two contradictory rules within a few months.

Read the section that matches what you are touching, not the whole file (~5,700 tokens):

| Section | Approx. lines |
|---|---|
| `## What this is` | 3–8 |
| `## No tests, no lint gate` | 22–33 |
| `## Backend` | 34–133 |
| `## Frontend` | 134–245 |
| `## Docker` | 246–307 |
| `## Production (the VPS)` | 308–364 |
| `## Known issues` | 371–376 |

`ISSUES.md` is the last full audit, and it is **history rather than current state**: most Critical
and High items are fixed, and some name files that no longer exist (`CharityCaseService.java`,
`frontend/src/**`). Read it for context on anything adjacent to your change, then verify against
source before acting on it.

## What this is

یاری‌جو is an **information board** for charity requests. Charity centres publish needs they have
verified; visitors browse and contact the centre directly. **There is no online payment and no
donation tracking.** Two roles, `ADMIN` and `CENTER`, and there is no public registration —
admins create centre accounts.

```
charity/
├── backend/     Spring Boot 3.3.5 REST API (Java 21, MySQL/H2, Flyway)
└── frontend/    Nuxt 3 SSR site (Vue 3, Tailwind v4, TypeScript, RTL Persian)
```

## The gates

These two are the only automated checks that exist:

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

H2 on port 8085, schema built from the real Flyway migrations, entities validated against it. A
clean start proves the migrations and the entity model agree.

```bash
cd frontend && npm run build
```

**`npm run lint` and `npm run typecheck` both abort** — no flat ESLint config was ever committed
and there is no root tsconfig. Their failure says nothing about your change.

There are **zero tests**, and CI does not run any. `.github/workflows/docker.yml` has two jobs.

`build-and-push` fires on every push to `master` or `development`, on PRs to `master`, and on a
manual `workflow_dispatch`. It builds both images (`mvn clean package -DskipTests` and
`npm run build`) and pushes them to GHCR — `:latest` from `master`, `:dev` from `development`.
So it catches a compile error or a broken build and nothing subtler.

`deploy` is the one that matters. It is guarded by
`github.ref == 'refs/heads/master' && github.event_name == 'push'`, so it is skipped on a
`development` push, on a pull request and on a manual dispatch — but **a push to `master` is a
production deployment**. It SSHes into the VPS, pulls the new images and runs
`docker compose -f compose.prod.yaml up -d backend frontend nginx`. Treat pushing `master` as a
release, not as a save.

Do not invent a gate that is not on this page.

## Subagents

Five specialists live in `.claude/agents/`. None pins a model, so all follow the session's.

| Agent | Use it for | Writes? |
|---|---|---|
| `architect` | Planning a change that crosses layers, alters status flow, adds a public endpoint or facet, changes caching/SSR, or introduces an entity | **No — by instruction** |
| `backend` | Java, JPA, security, announcements — **and the Flyway migration that accompanies an entity change** | Yes |
| `frontend` | Nuxt, SSR, Nitro caching, styling, routing | Yes |
| `tester` | Writing tests and reporting coverage gaps | Yes |
| `reviewer` | Reviewing a change for bugs, security, performance, drift — CRITICAL/HIGH/MEDIUM/LOW | **No — by instruction** |

`architect` and `reviewer` have no `Write` or `Edit`. That is a guardrail, not a wall: they do
carry `Bash`, which can write, so their read-only behaviour is a rule they keep rather than one
the tool list enforces. `Bash` is there for git and inspection.

**Invoke them deliberately, not by default.** A subagent starts with an empty context and
re-derives what this session already knows, so it costs *more* tokens than working inline. What
it buys is a clean main context and an independent perspective. That trade is worth it for broad
exploration, cross-cutting changes, and review — and not worth it for a one-file edit.

There is no separate database agent: schema work belongs to `backend`, because in this repo a
migration never arrives alone. It always trails an entity change.

## Non-negotiables

The full list is in `AGENTS.md`. These are the ones that cause real harm if broken:

- **A request carries no beneficiary name, contact detail, city or deadline.** Location and phone
  are read through `request → center → city`. The privacy page promises this.
- **`COMPLETED` is terminal.** Admin's `COMPLETED → INACTIVE` is the single exception.
- **An admin's takedown outranks a centre**, via `requests.deactivated_by`.
- **Deletion is soft** — a removed request answers **410**.
- **Flyway owns the schema on every profile.** Change an entity, add a migration.
- **Never put an `swr`/`isr` route rule on a Persian-slug path.**

## Git

`master` is production-ready, `development` is the active branch. Work on `development`.
Pushing `master` deploys to the VPS — see `## The gates`. Merge into it deliberately.
