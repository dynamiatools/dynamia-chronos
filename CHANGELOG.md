# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versionado siguiendo SemVer. Ver [ROADMAP.md](ROADMAP.md) para el plan completo
de las próximas versiones.

## [Unreleased]

Trabajo de la v1.2.0 en progreso — ver [TASKS.md](TASKS.md) para el detalle.

### Added
- HTTP client compartido y configurable (`chronos.http.thread-pool-size`,
  `chronos.http.connect-timeout`) para todas las ejecuciones de cron jobs,
  reemplazando el `HttpClient` nuevo por ejecución que no tenía límite de
  threads. El `serverTimeout` de cada job sigue mandando sobre el timeout de
  request/response, sin importar la config global del cliente.

### Planned
- Distributed locking del scheduler (ShedLock) para soportar múltiples réplicas.
- Retry con backoff configurable por `CronJob`.
- Métricas Prometheus/Micrometer + health probes.
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
