# Roadmap

Two-phase plan. Phase 1 (v1.2.0) is technical debt and plumbing: without it,
Phase 2 gets built on a foundation that doesn't scale horizontally and has no
schema control. Phase 2 (v1.3.0 → v1.5.0) is user-facing features and formal
Docker packaging.

Current status: `1.2.0` (released). No git tags yet (`git tag` is empty).

---

## v1.2.0 — Scalability and groundwork (no user-facing features) ✅ DONE

Goal: get Chronos to run on 2+ replicas without duplicating executions, and
give any Phase 2 feature something to stand on (versioned schema, metrics,
retry hook, configurable executor).

| # | Item | Why | Risk if skipped |
|---|------|-----|------------------|
| 1 | **Distributed locking** in `CronJobScheduler` via [ShedLock](https://github.com/lukas-krecan/ShedLock) (Apache 2.0, already designed for Spring `@Scheduled`) | Scheduling today lives in an in-memory `SimpleCache<Long, ScheduledTask>`, per instance. 2 replicas = the same job fired twice | Duplicate executions and notifications on any multi-pod deployment |
| 2 | **Retry/backoff** configurable per `CronJob` (`maxRetries`, `backoffMs`) in `CronJobsServiceImpl.execute()` | A network failure today gets logged and notified, no retry | False "service down" positives from transient timeouts |
| 3 | ~~Versioned schema migrations (Flyway)~~ — **dropped**, staying on `hibernate.ddl-auto=update` | Explicit decision: keep the current flow, no migration tool | Schema drift across environments stays a manual concern (see note below) |
| 4 | **Observability**: Actuator + Micrometer + Prometheus endpoint, custom metrics (`chronos_execution_duration_seconds`, `chronos_executions_total`, `chronos_next_execution_timestamp`) | Prerequisite for the analytics dashboard (v1.3) and for operating in k8s | Zero visibility into scheduler health in production |
| 5 | **Health probes** (`/actuator/health/liveness`, `/readiness`) | Required by k8s readiness/liveness before formalizing the Docker deployment (v1.5) | Rolling deploys without probes = downtime on every deploy |
| 6 | **Configurable executor/thread pool** via `application.yml` (HTTP pool size, global timeouts) | Lays the groundwork for per-project rate limiting (v1.3/1.4) | A tenant with many cron jobs can saturate the shared pool |
| 7 | **Integration tests with Testcontainers (MySQL)** for scheduler + locking + retry | `DynamiaChronosApplicationTests` used to require a reachable real DB — blocked CI. Testcontainers unblocks real GitHub Actions | Without real CI, every new feature is only validated by hand |

**Not in 1.2.0:** a formal Dockerfile (Cloud Native Buildpacks support already
exists, see README) — that's formalized in v1.5.0 alongside the rest of
deployment.

> **Note on schema:** Flyway was deliberately dropped — staying on
> `hibernate.ddl-auto=update` as-is today. Any feature adding
> columns/tables (retry, workflows, batches) follows the pattern already
> established in `CLAUDE.md` (`@Column(columnDefinition = "json")` +
> getters/setters for maps). No automatic schema rollback — reverting a
> field in production is manual.

**Workflow:** every item in this table (and every v1.3+ feature) is
developed on its own branch (`feature/<short-name>`), never directly on
`main`.

**Status:** all 6 items (1, 2, 4, 5, 6, 7) shipped, merged into
`integration/v1.2.0`, verified with a green 48/48 test run — including
`DynamiaChronosApplicationTests` booting the full Spring context against
real MySQL via Testcontainers. Pending: merge `integration/v1.2.0` into
`main`.

---

## v1.3.0 — Reliability-facing features

Builds directly on the v1.2.0 plumbing.

- **Retry UI**: expose `maxRetries`/`backoffMs` in `cronjob.zul` / `CronJobForm.yml`.
- **Simple workflows**: `dependsOnJobId` + condition (success/failure) on
  `CronJob`, reusing `CronJobExecutionListener` to chain executions.
- **Analytics dashboard v1**: success/failure and duration per project, using
  the already-persisted `CronJobLog` (embedded charts, no new infra).

## v1.4.0 — UI refresh + new login (visual redesign)

Login scope confirmed: **visual redesign only**, same auth mechanism as
`tools.dynamia.modules.security.ui` (own username/password). No
OAuth2/SSO/passkeys in this scope — stays a separate backlog item if needed
later.

- Login page redesign (branding, dark mode, better UX) without touching the
  auth backend.
- General UI refresh (ZK theme, dashboard, project tree).
- **Batches**: group N `CronJob`s with an aggregate notification once all
  complete.
- **HMAC signing** on outgoing notification webhooks (security,
  differentiator against JobRunr OSS).

## v1.5.0 — Formal Docker deployment

- Explicit multi-stage `Dockerfile` (build with `mvnw` + slim JRE runtime) as
  an alternative to Buildpacks, for anyone wanting a reproducible build
  without the buildpacks daemon.
- Reference `docker-compose.yml` (Chronos + MySQL/Percona).
- Image publishing to GHCR/Docker Hub via CI.
- Deployment docs (example k8s manifest using the v1.2.0 health probes).

---

## Out of scope (decided, not pursued)

- **Priority queues / worker autoscaling** (JobRunr Pro-style): doesn't apply —
  Chronos doesn't run its own processing, it fires HTTP calls with a timeout.
  There's no heavy-work queue to prioritize.
- **External job tracking** (human approval, GPU inference): out of scope for
  the product — Chronos fires HTTP, it doesn't run processing.
- **SSO/OAuth2/passkeys**: explicitly dropped for v1.4.0 (see above). Revisit
  as its own item if an enterprise customer asks for it.
