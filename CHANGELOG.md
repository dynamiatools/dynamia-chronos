# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows SemVer. See [ROADMAP.md](ROADMAP.md) for the full plan of
upcoming versions.

## [1.2.0] - 2026-08-05

### Added
- Distributed scheduler locking (ShedLock) — `CronJobScheduler` now runs
  every cron job under a lock keyed by `cronJob.id`, so running Chronos with
  multiple replicas no longer fires the same job twice. `shedlock` table
  created via `schema.sql` (no Flyway). Lock time (`lockAtMostFor`) accounts
  for retries: `serverTimeout * (maxRetries+1) + worst-case backoff`.
- Configurable retry with exponential backoff per `CronJob` (`maxRetries`,
  `retryBackoffMs`). `CronJobLog.attemptCount` records how many attempts a
  run took; the notification only fires once, after retries are exhausted or
  on success.
- Prometheus/Micrometer metrics: execution counter per project/result,
  duration timer, next-execution gauge per job. Actuator exposed on a
  separate management port (`MANAGEMENT_PORT`, default 9090), restricted by
  network instead of the app's login.
- k8s health probes (`/actuator/health/liveness`, `/readiness`), with
  readiness widened to include real DB connectivity (`db`), not just the
  app's startup state.
- Shared, configurable HTTP client (`chronos.http.thread-pool-size`,
  `chronos.http.connect-timeout`) for every cron job execution, replacing
  the per-execution `HttpClient` that had no thread-growth cap. Each job's
  `serverTimeout` still takes priority over the client's global config.
- Real CI via GitHub Actions (`.github/workflows/ci.yml`): `./mvnw test` on
  every push/PR against `ubuntu-latest`. `DynamiaChronosApplicationTests` now
  boots against real MySQL via Testcontainers instead of requiring a
  reachable external DB — runs in any environment with Docker.

### Fixed
- `LazyInitializationException` on startup against a real database, when an
  active `CronJob` has a `Project`: the next-execution gauge
  (`CronJobScheduler.refreshNextExecutionGauge()`) read
  `cronJob.getProject().getName()` after the Hibernate session that fetched
  the job list had already closed. `CronJobsServiceImpl.getActiveCronJobs()`
  now keeps the session open long enough to force-initialize the project.
  Only surfaced with real data — the Testcontainers suite's DB starts empty.

Note: Flyway was evaluated for schema versioning and dropped — staying on
`hibernate.ddl-auto=update`. See [ROADMAP.md](ROADMAP.md) for v1.3.0+.

## [1.1.0] - baseline

Project state at the start of this planning effort (no historical git tags).

### Added
- Custom HTTP headers per cron job (JSON-backed), with validation and tests.
- Postman-style request collection support (`RequestCollection`,
  `RequestItem`) with their own headers and parameters.
- `WebhookNotificationSender` for notifications via HTTP webhook.
- Large response handling (`@Lob`) in `ChronosHttpResponse`.
- Test mode/"Test" action to run a cron job manually without waiting for the
  cron trigger.
- Shared project tree between the dashboard and the projects page
  (`AbstractProjectsViewModel`).
- Postman collection import.
- Docker image build documentation via Cloud Native Buildpacks in the README.

### Fixed
- HTTP methods supported in `ChronosHttpRequestExecutor`.
- JSON serialization with date formatting.
