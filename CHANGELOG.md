# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versionado siguiendo SemVer. Ver [ROADMAP.md](ROADMAP.md) para el plan completo
de las próximas versiones.

## [1.2.0] - 2026-08-05

### Added
- Locking distribuido del scheduler (ShedLock) — `CronJobScheduler` ahora
  ejecuta cada cron job bajo un lock por `cronJob.id`, así que correr Chronos
  con múltiples réplicas ya no dispara el mismo job duplicado. Tabla
  `shedlock` creada vía `schema.sql` (sin Flyway). El tiempo de lock
  (`lockAtMostFor`) contempla reintentos: `serverTimeout * (maxRetries+1) +
  backoff en el peor caso`.
- Retry con backoff exponencial configurable por `CronJob` (`maxRetries`,
  `retryBackoffMs`). `CronJobLog.attemptCount` registra cuántos intentos tomó
  una ejecución; la notificación solo se dispara una vez, tras agotar
  reintentos o al éxito.
- Métricas Prometheus/Micrometer: contador de ejecuciones por proyecto/resultado,
  timer de duración, gauge de próxima ejecución por job. Actuator expuesto en
  un puerto de management separado (`MANAGEMENT_PORT`, default 9090),
  restringido por red en vez de por login de la app.
- Health probes de k8s (`/actuator/health/liveness`, `/readiness`), con
  readiness ampliado para incluir conectividad real a la BD (`db`), no solo
  el estado de arranque de la app.
- HTTP client compartido y configurable (`chronos.http.thread-pool-size`,
  `chronos.http.connect-timeout`) para todas las ejecuciones de cron jobs,
  reemplazando el `HttpClient` nuevo por ejecución que no tenía límite de
  threads. El `serverTimeout` de cada job sigue mandando sobre el timeout de
  request/response, sin importar la config global del cliente.
- CI real vía GitHub Actions (`.github/workflows/ci.yml`): `./mvnw test` en
  cada push/PR contra `ubuntu-latest`. `DynamiaChronosApplicationTests` ahora
  bootea contra MySQL real en Testcontainers en vez de requerir una BD
  externa alcanzable — corre en cualquier entorno con Docker.

Nota: se evaluó Flyway para versionar el schema y se descartó — se sigue con
`hibernate.ddl-auto=update`. Ver [ROADMAP.md](ROADMAP.md) para v1.3.0+.

## [1.1.0] - baseline

Estado del proyecto al iniciar esta planeación (sin tags históricos en git).

### Added
- Headers HTTP custom por cron job (JSON-backed), con validación y tests.
- Soporte de colecciones de requests estilo Postman (`RequestCollection`,
  `RequestItem`) con headers y parámetros propios.
- `WebhookNotificationSender` para notificaciones vía HTTP webhook.
- Manejo de respuestas grandes (`@Lob`) en `ChronosHttpResponse`.
- Modo test/"Test" action para ejecutar un cron job manualmente sin esperar el cron.
- Árbol de proyectos compartido entre dashboard y página de proyectos
  (`AbstractProjectsViewModel`).
- Importación de colecciones Postman.
- Documentación de build de imagen Docker vía Cloud Native Buildpacks en el README.

### Fixed
- Métodos HTTP soportados en `ChronosHttpRequestExecutor`.
- Serialización JSON con formato de fechas.
