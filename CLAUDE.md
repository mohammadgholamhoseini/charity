# CLAUDE.md

## Read `AGENTS.md` first

**`AGENTS.md` is the source of truth for this project's domain rules.** It is ~405 lines and
nearly every bullet records a bug that already shipped. Nothing in it is repeated here, on
purpose — two copies of a rule become two contradictory rules within a few months.

Read the section that matches what you are touching, not the whole file (~6,100 tokens):

| Section | Approx. lines |
|---|---|
| `## What this is` | 3–8 |
| `## No tests, no lint gate` | 22–33 |
| `## Backend` | 34–133 |
| `## Frontend` | 134–245 |
| `## Docker` | 246–307 |
| `## Production (the VPS)` | 308–369 |
| `## Git` | 370–400 |
| `## Known issues` | 401–406 |

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

Six specialists live in `.claude/agents/`. None pins a model, so all follow the session's.

| Agent | Use it for | Writes? |
|---|---|---|
| `architect` | Planning a change that crosses layers, alters status flow, adds a public endpoint or facet, changes caching/SSR, or introduces an entity | **No — by instruction** |
| `backend` | Java, JPA, security, announcements — **and the Flyway migration that accompanies an entity change** | Yes |
| `frontend` | Nuxt, SSR, Nitro caching, styling, routing | Yes |
| `tester` | Writing tests and reporting coverage gaps | Yes |
| `reviewer` | Reviewing a change for bugs, security, performance, drift — CRITICAL/HIGH/MEDIUM/LOW | **No — by instruction** |
| `tasks` | Reading and writing the Trello board — proposing the next card, working one up into a brief, recording an outcome | Board only — **never the code** |

`architect` and `reviewer` have no `Write` or `Edit`. That is a guardrail, not a wall: they do
carry `Bash`, which can write, so their read-only behaviour is a rule they keep rather than one
the tool list enforces. `Bash` is there for git and inspection.

**Invoke them deliberately, not by default.** A subagent starts with an empty context and
re-derives what this session already knows, so it costs *more* tokens than working inline. What
it buys is a clean main context and an independent perspective. That trade is worth it for broad
exploration, cross-cutting changes, and review — and not worth it for a one-file edit.

`tasks` is the board, not an orchestrator. It cannot ask you anything and it cannot invoke another
agent — subagents do neither — so it *proposes* a card and stops. Approval and dispatch stay in the
main session: `tasks` → you approve → `architect` (when the change earns one) → `backend`/`frontend`
→ `reviewer` → `tasks` again to close the card. Its credentials come from `~/.trello.env`, outside
the repository on purpose; `.claude/scripts/README-trello.md` covers setup.

There is no separate database agent: schema work belongs to `backend`, because in this repo a
migration never arrives alone. It always trails an entity change.

## Non-negotiables

The full list is in `AGENTS.md`. These are the ones that cause real harm if broken:

- **A request carries no contact detail, city or deadline of its own.** Location and phone are
  read through `request → center → city`.
- **The platform never asks for or stores beneficiary identity.** Whether to *name* a beneficiary
  inside the free-text description is the centre's own call, under written consent — that changed
  in August 2026 and `AGENTS.md` records why. Do not restore the old absolute rule.
- **`COMPLETED` is terminal.** Admin's `COMPLETED → INACTIVE` is the single exception.
- **An admin's takedown outranks a centre**, via `requests.deactivated_by`.
- **Deletion is soft** — a removed request answers **410**.
- **Flyway owns the schema on every profile.** Change an entity, add a migration.
- **Never put an `swr`/`isr` route rule on a Persian-slug path.**

## Git

`master` is production-ready, `development` is the active branch. Work on `development`.
Pushing `master` deploys to the VPS — see `## The gates`. Merge into it deliberately.

**A commit that closes a card names it.** Not one card per commit — that rule breaks in both
directions and pretending otherwise just produces dishonest history. `62394d0` closed three cards
at once because all three touched one component and splitting them would have meant three commits
that do not stand alone; conversely a card like "write tests" is obviously many commits. What is
worth keeping is the *trace*, in both directions:

```
Trello: https://trello.com/c/NsEILCEA
```

as a trailer in the commit message — repeat the line when a commit closes more than one card —
and the commit recorded back on the card. The wrapper does both halves:

```bash
bash .claude/scripts/trello.sh trailer <cardId>      # before committing: prints the trailer line
bash .claude/scripts/trello.sh link <cardId> <sha>   # after committing: comments the sha on the card
```

There is deliberately **no `commit-msg` hook enforcing this**. Plenty of commits have no card —
this paragraph does not — and a hook that has to be bypassed weekly teaches people to bypass it.
