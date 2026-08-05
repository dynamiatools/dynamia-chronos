# Roadmap

Plan de dos fases. Fase 1 (v1.2.0) es deuda técnica y plomería: sin eso, la Fase 2
se construye sobre una base que no escala horizontalmente y sin control de schema.
Fase 2 (v1.3.0 → v1.5.0) son features visibles y el empaquetado Docker formal.

Estado actual: `1.1.0`. Sin releases etiquetados en git todavía (`git tag` vacío).

---

## v1.2.0 — Escalabilidad y preparación (sin features de usuario)

Objetivo: que Chronos pueda correr en 2+ réplicas sin duplicar ejecuciones, y que
cualquier feature de la Fase 2 tenga dónde pararse (schema versionado, métricas,
retry hook, executor configurable).

| # | Item | Por qué | Riesgo si se omite |
|---|------|---------|---------------------|
| 1 | **Distributed locking** en `CronJobScheduler` vía [ShedLock](https://github.com/lukas-krecan/ShedLock) (Apache 2.0, ya piensa en Spring `@Scheduled`) | Hoy el scheduling vive en `SimpleCache<Long, ScheduledTask>` en memoria, por instancia. 2 réplicas = mismo job disparado 2 veces | Ejecuciones y notificaciones duplicadas en cualquier despliegue multi-pod |
| 2 | **Retry/backoff** configurable por `CronJob` (`maxRetries`, `backoffMs`) en `CronJobsServiceImpl.execute()` | Hoy un fallo de red se loguea y notifica, sin reintento | Falsos positivos de "servicio caído" por timeouts transitorios |
| 3 | ~~Migración de schema versionada (Flyway)~~ — **descartado**, se sigue con `hibernate.ddl-auto=update` | Decisión explícita: mantener el flujo actual, sin herramienta de migraciones | Drift de schema entre entornos sigue siendo responsabilidad manual (ver nota abajo) |
| 4 | **Observabilidad**: Actuator + Micrometer + endpoint Prometheus, métricas custom (`chronos_execution_duration_seconds`, `chronos_executions_total`, `chronos_next_execution_timestamp`) | Prerequisito para dashboards de analítica (v1.3) y para operar en k8s | Cero visibilidad de salud del scheduler en producción |
| 5 | **Health probes** (`/actuator/health/liveness`, `/readiness`) | Requerido por k8s readiness/liveness antes de formalizar despliegue Docker (v1.5) | Rolling deploys sin probes = downtime en despliegues |
| 6 | **Executor/thread-pool configurable** vía `application.yml` (tamaño de pool HTTP, timeouts globales) | Prepara el terreno para rate limiting por proyecto (v1.3/1.4) | Un tenant con muchos cron jobs puede saturar el pool compartido |
| 7 | **Tests de integración con Testcontainers (MySQL)** para scheduler + locking + retry | `DynamiaChronosApplicationTests` hoy exige una BD real alcanzable — bloquea CI. Testcontainers desbloquea GitHub Actions real | Sin CI real, cada feature nueva se valida solo a mano |

**No entra en 1.2.0:** Dockerfile formal (ya existe soporte vía Cloud Native
Buildpacks, ver README) — se formaliza en v1.5.0 junto al resto de deployment.

> **Nota sobre schema:** se descartó Flyway a propósito — se sigue con
> `hibernate.ddl-auto=update` tal como está hoy. Cada feature que añada
> columnas/tablas (retry, workflows, batches) sigue el patrón ya establecido
> en `CLAUDE.md` (`@Column(columnDefinition = "json")` + getters/setters para
> mapas). Sin rollback automático de schema — si se necesita revertir un campo
> en producción, es manual.

**Flujo de trabajo:** cada item de esta tabla (y cada feature de v1.3+) se
desarrolla en su propia rama (`feature/<nombre-corto>`), no directo sobre `main`.

---

## v1.3.0 — Reliability-facing features

Construye directamente sobre la plomería de 1.2.0.

- **UI de retry**: exponer `maxRetries`/`backoffMs` en `cronjob.zul` / `CronJobForm.yml`.
- **Workflows simples**: `dependsOnJobId` + condición (success/failure) en `CronJob`,
  reutilizando `CronJobExecutionListener` para encadenar ejecuciones.
- **Dashboard analítico v1**: éxito/fallo y duración por proyecto, usando
  `CronJobLog` ya persistido (gráficas embebidas, sin nueva infra).

## v1.4.0 — UI refresh + nuevo login (rediseño visual)

Alcance de login confirmado: **solo rediseño visual**, mismo mecanismo de auth de
`tools.dynamia.modules.security.ui` (usuario/password propio). Sin OAuth2/SSO/passkeys
en este alcance — queda como ítem de backlog separado si se necesita más adelante.

- Rediseño de la página de login (branding, dark mode, mejor UX) sin tocar el
  backend de autenticación.
- Refresh de UI general (tema ZK, dashboard, árbol de proyectos).
- **Batches**: agrupar N `CronJob`s con notificación agregada al completar todos.
- **Firma HMAC** en webhooks salientes de notificación (seguridad, diferenciador
  frente a JobRunr OSS).

## v1.5.0 — Despliegue Docker formal

- `Dockerfile` multi-stage explícito (build con `mvnw` + runtime JRE slim) como
  alternativa a Buildpacks, para quienes quieren build reproducible sin daemon
  de buildpacks.
- `docker-compose.yml` de referencia (Chronos + MySQL/Percona).
- Publicación de imagen a GHCR/Docker Hub vía CI.
- Documentación de despliegue (k8s manifest de ejemplo usando los health
  probes de v1.2.0).

---

## Fuera de alcance (decidido, no se persigue)

- **Priority queues / autoscaling de workers** (tipo JobRunr Pro): no aplica —
  Chronos no ejecuta processing propio, dispara HTTP con timeout. No hay cola
  de trabajo pesado que priorizar.
- **External job tracking** (aprobación humana, GPU inference): fuera del
  producto — Chronos dispara HTTP, no ejecuta processing.
- **SSO/OAuth2/passkeys**: descartado explícitamente para v1.4.0 (ver arriba).
  Revisar como ítem propio si un cliente enterprise lo pide.
