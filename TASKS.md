# Tasks

Checklist accionable por versión. Ver [ROADMAP.md](ROADMAP.md) para el contexto/porqué
de cada item y [CHANGELOG.md](CHANGELOG.md) para lo ya entregado.

Convención: `[ ]` pendiente, `[~]` en progreso, `[x]` hecho.

Cada item numerado de v1.2.0 y cada feature de v1.3+ vive en su propia rama
`feature/<nombre-corto>` (ver mapeo de ramas al final del archivo), nunca
directo sobre `main`.

---

## v1.2.0 — Escalabilidad y preparación

### 1. Distributed locking (scheduler)
- [ ] Agregar dependencia `shedlock-spring` + `shedlock-provider-jdbc-template`.
- [ ] Crear tabla `shedlock` (vía migración Flyway, ver bloque 3).
- [ ] Envolver la ejecución programada en `CronJobScheduler` con `@SchedulerLock`
      (lockAtMostFor/lockAtLeastFor acorde a `serverTimeout` del job).
- [ ] Test de integración: 2 instancias simuladas del scheduler contra la misma
      BD → solo una ejecuta el job.

### 2. Retry / backoff — `feature/cronjob-retry-backoff`
- [x] Columnas `maxRetries` (int, default 0) y `retryBackoffMs` (long, default
      0) en `CronJob`, con setters que clampean negativos a 0.
- [x] Lógica de reintento en `CronJobsServiceImpl`: extraída a
      `runWithRetry(...)`, un método estático puro (sin `crudService()`)
      desacoplado de la persistencia y del executor — reintenta mientras
      `response.isFail()` y queden intentos. Backoff **exponencial**
      (`backoffMs * 2^(attempt-1)`, cap de 60s) vía `nextBackoffMs(...)`.
      Test mode (`test()`) nunca reintenta — feedback inmediato de un solo intento.
- [x] `CronJobLog.attemptCount` registra cuántos intentos tomó (1 = sin
      reintento). Un solo row de log por trigger, no uno por intento fallido.
- [x] Notificación (`fireAfterExecutionListener`) sigue disparándose una sola
      vez, después de agotar reintentos o del éxito — nunca en intentos
      intermedios, porque el loop vive antes del punto donde se dispara.
- [x] Tests: `CronJobsServiceImplRetryTest` (0 reintentos, éxito en intento k,
      reintentos agotados, backoff exponencial con cap) + `CronJobTest`
      (defaults, clamping de negativos).

> **Nota para la integración con `feature/scheduler-distributed-lock`:** los
> reintentos alargan la duración total de una ejecución más allá de
> `serverTimeout`. Al mergear ambas ramas, `CronJobScheduler.executeWithLock`
> debe calcular `lockAtMostFor` sobre `serverTimeout * (maxRetries + 1) +
> backoff total`, no solo `serverTimeout` — si no, ShedLock puede soltar el
> lock antes de que termine el último reintento.

### 3. ~~Migraciones versionadas (Flyway)~~ — descartado
Se mantiene `hibernate.ddl-auto=update`. Sin tasks aquí; cada feature que
añada columnas sigue el patrón ya documentado en `CLAUDE.md`.

### 4. Observabilidad
- [ ] Agregar `spring-boot-starter-actuator` + `micrometer-registry-prometheus`.
- [ ] Exponer `/actuator/prometheus`.
- [ ] Métricas custom: contador de ejecuciones (éxito/fallo) por proyecto,
      timer de duración de ejecución, gauge de próxima ejecución por job.
- [ ] Decidir y documentar qué endpoints de Actuator quedan expuestos sin auth
      (mínimo: health, prometheus detrás de red interna).

### 5. Health probes
- [ ] Habilitar `management.endpoint.health.probes.enabled=true`.
- [ ] Verificar que liveness/readiness reflejen conectividad real a la BD.
- [ ] Documentar probes de ejemplo (aunque el manifest k8s completo es v1.5.0).

### 6. Executor configurable
- [ ] Externalizar tamaño de thread pool HTTP y timeouts globales a
      `application.yml` (con defaults sensatos, override vía env var).
- [ ] Confirmar que `serverTimeout` por job sigue teniendo prioridad sobre el
      timeout global.

### 7. CI real con Testcontainers
- [ ] Introducir Testcontainers MySQL en el módulo de test.
- [ ] Separar `DynamiaChronosApplicationTests` (hoy requiere BD real externa)
      para que corra contra el contenedor, no contra una BD fija.
- [ ] Agregar GitHub Actions workflow: `./mvnw test` en cada PR.

---

## v1.3.0 — Reliability-facing features

- [ ] UI: campos `maxRetries`/`backoffMs` en `cronjob.zul` y `CronJobForm.yml`.
- [ ] Modelo: columna `depends_on_job_id` (nullable) + condición
      (`ON_SUCCESS`/`ON_FAILURE`) en `CronJob`.
- [ ] `CronJobExecutionListener`: al completar un job, evaluar si dispara jobs
      dependientes.
- [ ] Guardas contra ciclos en la cadena de dependencias (validación al guardar).
- [ ] Dashboard: vista de tasa de éxito y duración (percentiles) por proyecto,
      usando `CronJobLog`.

## v1.4.0 — UI refresh + login (rediseño visual)

- [ ] Rediseño de la página de login (branding, dark mode) — sin tocar
      `tools.dynamia.modules.security.ui` como mecanismo de auth.
- [ ] Refresh de tema ZK en dashboard y árbol de proyectos.
- [ ] Batches: entidad/agrupación de `CronJob`s con notificación agregada al
      completar todos los jobs del batch.
- [ ] Firma HMAC en payloads de `WebhookNotificationSender` (header
      `X-Chronos-Signature`, secreto configurable por notificador).

## v1.5.0 — Despliegue Docker formal

- [ ] `Dockerfile` multi-stage (build `mvnw package` → runtime JRE slim).
- [ ] `docker-compose.yml` de referencia (Chronos + MySQL/Percona).
- [ ] Pipeline CI para build + push de imagen a GHCR/Docker Hub en release.
- [ ] Manifest k8s de ejemplo (Deployment + probes de v1.2.0 + Service).
- [ ] Actualizar README: sección Docker con Dockerfile explícito como
      alternativa a Buildpacks.

---

## Fuera de alcance (no crear tasks para esto salvo que cambie la decisión)

- Priority queues / autoscaling de workers.
- External job tracking (aprobación humana, GPU inference).
- SSO/OAuth2/passkeys en login (revisar como iniciativa aparte si aplica).
- Flyway / migraciones versionadas — se sigue con `hibernate.ddl-auto=update`.

---

## Mapeo rama → item

| Rama | Item |
|------|------|
| `feature/scheduler-distributed-lock` | v1.2.0 #1 — ShedLock |
| `feature/cronjob-retry-backoff` | v1.2.0 #2 — Retry/backoff |
| `feature/observability-metrics` | v1.2.0 #4 — Actuator/Micrometer/Prometheus |
| `feature/health-probes` | v1.2.0 #5 — liveness/readiness |
| `feature/executor-config` | v1.2.0 #6 — thread pool configurable |
| `feature/testcontainers-ci` | v1.2.0 #7 — Testcontainers + CI |
| `feature/cronjob-workflows` | v1.3.0 — dependencias entre jobs |
| `feature/dashboard-analytics` | v1.3.0 — analítica de ejecuciones |
| `feature/login-redesign` | v1.4.0 — rediseño visual login |
| `feature/ui-refresh` | v1.4.0 — refresh general de UI |
| `feature/notification-batches` | v1.4.0 — batches |
| `feature/webhook-hmac` | v1.4.0 — firma HMAC |
| `feature/docker-deployment` | v1.5.0 — Dockerfile + compose + CI publish |
