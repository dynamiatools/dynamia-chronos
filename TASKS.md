# Tasks

Actionable checklist per version. See [ROADMAP.md](ROADMAP.md) for the
why/context of each item and [CHANGELOG.md](CHANGELOG.md) for what's already
shipped.

Convention: `[ ]` pending, `[~]` in progress, `[x]` done.

Every numbered v1.2.0 item (and every v1.3+ feature) lives on its own branch
(`feature/<short-name>`, see the branch map at the end of this file), never
directly on `main`.

---

## v1.2.0 — Scalability and groundwork ✅ COMPLETE

All branches merged into `integration/v1.2.0` (1→7, item 3 dropped by
decision). Full suite green (49/49), including
`DynamiaChronosApplicationTests` against real MySQL via Testcontainers.
Pending: merge `integration/v1.2.0` into `main`.

### Post-merge fix: `LazyInitializationException` on real DB startup
Manual testing against a real MySQL instance surfaced a bug the
Testcontainers suite couldn't catch (its DB starts empty, so
`getActiveCronJobs()` had nothing to iterate): with a real active `CronJob`
that has a `Project`, app startup crashed with
`org.hibernate.LazyInitializationException: Could not initialize proxy
[Project#1] - no session`.

Root cause: `CronJobScheduler.refreshNextExecutionGauge()` (added in item 4,
Observability) reads `cronJob.getProject().getName()` for the metric's tag,
but `CronJobsServiceImpl.getActiveCronJobs()` only wrapped the `find()` query
in a session — the lazy `Project` proxy was touched later, after that
session had already closed. At app startup this runs outside any web
request, so there's no Open-Session-In-View to paper over it.

Fix: `getActiveCronJobs()` now wraps the query **and** a force-touch of
`cronJob.getProject().getName()` in the same `crudService().executeWithinTransaction(...)`
block, so the session stays open long enough. Added
`DynamiaChronosApplicationTests#reschedulingActiveJobsWithAProjectDoesNotFailOnLazyLoading`,
which seeds a real `Project` + active `CronJob` and calls
`cronJobScheduler.reload()` — verified it reproduces the exact stack trace
with the fix reverted, and passes with it applied.

### 1. Distributed locking (scheduler) — `feature/scheduler-distributed-lock`
- [x] Added `shedlock-spring` + `shedlock-provider-jdbc-template` (7.7.0) dependency.
- [x] Created the `shedlock` table via `src/main/resources/schema.sql`
      (`CREATE TABLE IF NOT EXISTS`, `spring.sql.init.mode: always`) — no
      Flyway, consistent with the decision to stay on `ddl-auto: update`.
- [x] Wrapped scheduled execution in `CronJobScheduler`. Design note: jobs
      are registered dynamically via `ScheduledTaskRegistrar` (per-entity
      cron expression, not a fixed method), so `@SchedulerLock` (AOP over
      annotated methods) doesn't apply — used the programmatic API
      (`LockingTaskExecutor`/`DefaultLockingTaskExecutor`) in
      `CronJobScheduler.executeWithLock()` instead. `lockAtMostFor` =
      job's `serverTimeout` + 15s margin, floored at 30s; `lockAtLeastFor` = 0.
- [x] Simulated-instances test: `CronJobSchedulerLockTest` spins up 2
      `CronJobScheduler`s (2 independent `LockProvider`s) against the same
      `shedlock` table on in-memory H2, firing `executeWithLock` in parallel
      → only one executes the job. Doesn't need real MySQL (that's covered
      by the Testcontainers suite in `feature/testcontainers-ci`).

### 2. Retry / backoff — `feature/cronjob-retry-backoff`
- [x] `maxRetries` (int, default 0) and `retryBackoffMs` (long, default 0)
      columns on `CronJob`, with setters that clamp negatives to 0.
- [x] Retry logic in `CronJobsServiceImpl`: extracted into `runWithRetry(...)`,
      a pure static method (no `crudService()`) decoupled from persistence
      and from the executor — retries while `response.isFail()` and attempts
      remain. **Exponential** backoff (`backoffMs * 2^(attempt-1)`, capped
      at 60s) via `nextBackoffMs(...)`. Test mode (`test()`) never retries —
      immediate single-attempt feedback.
- [x] `CronJobLog.attemptCount` records how many attempts it took (1 = no
      retry). Still one log row per trigger, not one per failed attempt.
- [x] Notification (`fireAfterExecutionListener`) still fires exactly once,
      after retries are exhausted or on success — never on intermediate
      attempts, because the loop runs before the point where it fires.
- [x] Tests: `CronJobsServiceImplRetryTest` (no retry, success at attempt k,
      exhausted retries, exponential backoff with cap) + `CronJobTest`
      (defaults, negative clamping).

> **Note for integration with `feature/scheduler-distributed-lock`:**
> retries extend a run's total duration beyond `serverTimeout`. When
> merging both branches, `CronJobScheduler.executeWithLock` must compute
> `lockAtMostFor` from `serverTimeout * (maxRetries + 1) + total backoff`,
> not just `serverTimeout` — otherwise ShedLock could release the lock
> before the last retry finishes.

### 3. ~~Versioned migrations (Flyway)~~ — dropped
Staying on `hibernate.ddl-auto=update`. No tasks here; any feature adding
columns follows the pattern already documented in `CLAUDE.md`.

### 4. Observability — `feature/observability-metrics`
- [x] `spring-boot-starter-actuator` was already present; added
      `micrometer-registry-prometheus`.
- [x] `/actuator/prometheus` exposed — **decision**: Actuator runs on its own
      port (`management.server.port`, default 9090 via `MANAGEMENT_PORT`),
      separate from `server.port`. Not secured behind the app's login:
      restricted by network instead (no public ingress / intra-cluster-only
      k8s Service). `include: health,prometheus,info`.
- [x] Custom metrics:
      - Counter `chronos_cronjob_executions_total{project,result}` — per
        project and result (success/failure), in `CronJobsServiceImpl`.
      - Timer `chronos_cronjob_execution_duration_seconds{project}`.
      - Gauge `chronos_cronjob_next_execution_timestamp_seconds{job,project}`
        via `MultiGauge` in `CronJobScheduler`, recomputed on every
        scheduling change (`MultiGauge` has no per-row upsert, the whole set
        gets rebuilt — cheap since it only runs on job create/update/delete,
        not on every tick).
      - Test-mode runs (the "Test" action) are excluded from metrics so
        manual pokes don't skew real success-rate/duration numbers.
- [x] Tests: `CronJobTest` (new cases for `getNextExecutionDateTime()`).
      Actual metrics recording (`recordMetrics`) has no dedicated unit test —
      it depends on `crudService()`/Spring context like the rest of
      `CronJobsServiceImpl`; covered by manual verification and, later, by
      the `feature/testcontainers-ci` suite.

### 5. Health probes — `feature/health-probes`
- [x] `management.endpoint.health.probes.enabled=true` → exposes
      `/actuator/health/liveness` and `/actuator/health/readiness`.
- [x] Readiness widened past Spring Boot's default (`readinessState` only):
      `group.readiness.include: readinessState,db`. Explicit decision —
      Chronos can't do its job (running HTTP jobs and persisting the
      result) without the DB, so a broken DB should pull the pod out of the
      k8s Service, not just leave it marked "started". The `db` indicator
      comes for free from `DataSourceHealthContributorAutoConfiguration`
      (a real connection-validation query) — no custom HealthIndicator needed.
- [x] Lives on the same separate management port as
      `feature/observability-metrics` (same `management:` block, reconciled
      when both branches were integrated).
- [ ] Example k8s manifest with these probes: deferred to
      `feature/docker-deployment` (v1.5.0), where the rest of the manifest lives.

> **Note:** this feature is 100% configuration (`application.yml`), no Java
> code. Verifying that readiness actually flips when the DB stops responding
> needs a real Spring + Actuator + DB context up — there's no honest way to
> cover that with a pure unit test like the rest of the repo; it stays a
> manual check (`curl` the endpoint with the DB down) and, eventually, a case
> in the `feature/testcontainers-ci` suite.

### 6. Configurable executor — `feature/executor-config`
- [x] `ChronosHttpExecutorProperties` (`chronos.http.thread-pool-size` /
      `chronos.http.connect-timeout`, overridable via
      `CHRONOS_HTTP_THREAD_POOL_SIZE`/`CHRONOS_HTTP_CONNECT_TIMEOUT`) +
      `ChronosHttpClientConfig` building a shared `HttpClient` over a
      fixed-size `ExecutorService`. Replaces the `HttpClient.newHttpClient()`
      that was created fresh (with its own internal cached thread pool) on
      **every** execution — there used to be no cap on thread growth under
      an "every job fires at :00" spike.
- [x] `ChronosHttpRequestExecutor`/`CronJobHttpRequestExecutor` gain a 4-arg
      constructor taking the `HttpClient`; the 3-arg one (backward
      compatibility, used by tests and by the manual "send request" action in
      `RequestItemViewModel`) uses a lazy, JVM-wide default client, not
      Spring-managed.
- [x] Confirmed: per-job `serverTimeout` still governs the
      *request/response* timeout (`HttpRequest.Builder.timeout(...)`)
      regardless of the shared client's config — the global `connectTimeout`
      is a different dimension (TCP connect only). Test
      `jobServerTimeoutTakesPriorityOverSharedClientConnectTimeout` proves
      it with a server that responds slowly but connects instantly.

### 7. Real CI with Testcontainers — `feature/testcontainers-ci`
- [x] Testcontainers MySQL introduced: `spring-boot-testcontainers` +
      `org.testcontainers:junit-jupiter`/`mysql`. **Version note**: the
      `testcontainers.version` pulled in by the Spring Boot parent (2.0.5)
      doesn't have the `junit-jupiter`/`mysql` modules published yet — pinned
      the property to `1.21.4` (the latest that does) in the pom's own
      `<properties>`, not per-dependency, so `testcontainers-core` and the
      modules stay on the same version. Revisit when bumping
      dynamiatools/Spring Boot, in case 2.x has published them by then.
- [x] `DynamiaChronosApplicationTests` now uses `@Testcontainers` +
      `@Container` + `@ServiceConnection` on a `MySQLContainer` — no longer
      depends on `DATABASE_URL`/a reachable external DB, just a Docker
      daemon. Verified running locally: boots the full context against real
      MySQL (~16-40s including the image pull).
- [x] GitHub Actions (`.github/workflows/ci.yml`): `./mvnw test` on every
      push to `main` and on every PR, on `ubuntu-latest` (Docker comes
      preinstalled, nothing else to provision).
- [x] `CLAUDE.md` updated: the "needs a reachable external DB" note for
      `DynamiaChronosApplicationTests` no longer applies.

---

## v1.3.0 — Reliability-facing features

- [ ] UI: `maxRetries`/`backoffMs` fields in `cronjob.zul` and `CronJobForm.yml`.
- [ ] Model: `depends_on_job_id` column (nullable) + condition
      (`ON_SUCCESS`/`ON_FAILURE`) on `CronJob`.
- [ ] `CronJobExecutionListener`: on job completion, evaluate whether to fire
      dependent jobs.
- [ ] Guards against cycles in the dependency chain (validated on save).
- [ ] Dashboard: success-rate and duration (percentiles) view per project,
      using `CronJobLog`.

## v1.4.0 — UI refresh + login (visual redesign)

- [ ] Login page redesign (branding, dark mode) — without touching
      `tools.dynamia.modules.security.ui` as the auth mechanism.
- [ ] ZK theme refresh on the dashboard and project tree.
- [ ] Batches: `CronJob` grouping entity with an aggregate notification once
      all jobs in the batch complete.
- [ ] HMAC signing on `WebhookNotificationSender` payloads
      (`X-Chronos-Signature` header, secret configurable per notificator).

## v1.5.0 — Formal Docker deployment

- [ ] Multi-stage `Dockerfile` (build `mvnw package` → slim JRE runtime).
- [ ] Reference `docker-compose.yml` (Chronos + MySQL/Percona).
- [ ] CI pipeline to build + push the image to GHCR/Docker Hub on release.
- [ ] Example k8s manifest (Deployment + v1.2.0 probes + Service).
- [ ] Update README: Docker section with an explicit Dockerfile as an
      alternative to Buildpacks.

---

## Out of scope (don't create tasks for this unless the decision changes)

- Priority queues / worker autoscaling.
- External job tracking (human approval, GPU inference).
- SSO/OAuth2/passkeys on login (revisit as a separate initiative if needed).
- Flyway / versioned migrations — staying on `hibernate.ddl-auto=update`.

---

## Branch → item map

| Branch | Item |
|--------|------|
| `feature/scheduler-distributed-lock` | v1.2.0 #1 — ShedLock |
| `feature/cronjob-retry-backoff` | v1.2.0 #2 — Retry/backoff |
| `feature/observability-metrics` | v1.2.0 #4 — Actuator/Micrometer/Prometheus |
| `feature/health-probes` | v1.2.0 #5 — liveness/readiness |
| `feature/executor-config` | v1.2.0 #6 — configurable thread pool |
| `feature/testcontainers-ci` | v1.2.0 #7 — Testcontainers + CI |
| `feature/cronjob-workflows` | v1.3.0 — job dependencies |
| `feature/dashboard-analytics` | v1.3.0 — execution analytics |
| `feature/login-redesign` | v1.4.0 — login visual redesign |
| `feature/ui-refresh` | v1.4.0 — general UI refresh |
| `feature/notification-batches` | v1.4.0 — batches |
| `feature/webhook-hmac` | v1.4.0 — HMAC signing |
| `feature/docker-deployment` | v1.5.0 — Dockerfile + compose + CI publish |
