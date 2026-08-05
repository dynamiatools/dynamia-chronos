# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versionado siguiendo SemVer. Ver [ROADMAP.md](ROADMAP.md) para el plan completo
de las próximas versiones.

## [Unreleased]

Trabajo de la v1.2.0 en progreso — ver [TASKS.md](TASKS.md) para el detalle.

### Added
- Locking distribuido del scheduler (ShedLock) — `CronJobScheduler` ahora
  ejecuta cada cron job bajo un lock por `cronJob.id`, así que correr Chronos
  con múltiples réplicas ya no dispara el mismo job duplicado. Tabla
  `shedlock` creada vía `schema.sql` (sin Flyway).
- Retry con backoff exponencial configurable por `CronJob` (`maxRetries`,
  `retryBackoffMs`). `CronJobLog.attemptCount` registra cuántos intentos tomó
  una ejecución; la notificación solo se dispara una vez, tras agotar
  reintentos o al éxito.

### Planned
- Métricas Prometheus/Micrometer + health probes.
- Executor/thread-pool configurable vía `application.yml`.
- Tests de integración con Testcontainers (MySQL).

Nota: se evaluó Flyway para versionar el schema y se descartó — se sigue con
`hibernate.ddl-auto=update`.

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
