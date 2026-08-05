# Tasks

Checklist accionable por versión. Ver [ROADMAP.md](ROADMAP.md) para el contexto/porqué
de cada item y [CHANGELOG.md](CHANGELOG.md) para lo ya entregado.

Convención: `[ ]` pendiente, `[~]` en progreso, `[x]` hecho.

Cada item numerado de v1.2.0 y cada feature de v1.3+ vive en su propia rama
`feature/<nombre-corto>` (ver mapeo de ramas al final del archivo), nunca
directo sobre `main`.

---

## v1.2.0 — Escalabilidad y preparación

### 1. Distributed locking (scheduler) — `feature/scheduler-distributed-lock`
- [x] Agregar dependencia `shedlock-spring` + `shedlock-provider-jdbc-template` (7.7.0).
- [x] Crear tabla `shedlock` vía `src/main/resources/schema.sql`
      (`CREATE TABLE IF NOT EXISTS`, `spring.sql.init.mode: always`) — sin
      Flyway, consistente con la decisión de seguir con `ddl-auto: update`.
- [x] Envolver la ejecución programada en `CronJobScheduler`. Nota de diseño:
      los jobs se registran dinámicamente vía `ScheduledTaskRegistrar`
      (cron expression por entidad, no un método fijo), así que `@SchedulerLock`
      (AOP sobre métodos anotados) no aplica — se usó la API programática
      (`LockingTaskExecutor`/`DefaultLockingTaskExecutor`) en
      `CronJobScheduler.executeWithLock()`. `lockAtMostFor` = `serverTimeout`
      del job + 15s de margen, con piso de 30s; `lockAtLeastFor` = 0.
- [x] Test de instancias simuladas: `CronJobSchedulerLockTest` levanta 2
      `CronJobScheduler` (2 `LockProvider` independientes) contra la misma
      tabla `shedlock` en H2 in-memory y dispara `executeWithLock` en paralelo
      → solo una ejecuta el job. No requiere MySQL real (eso queda para la
      suite de Testcontainers de `feature/testcontainers-ci`).

### 2. Retry / backoff
- [ ] Agregar columnas `max_retries` (int, default 0) y `backoff_ms` (int,
      default 0) a `CronJob`.
- [ ] Lógica de reintento en `CronJobsServiceImpl.execute()`: reintentar sobre
      fallo de `ChronosHttpRequestExecutor` hasta `maxRetries`, con backoff
      (fijo o exponencial simple — decidir al implementar).
- [ ] `CronJobLog` debe registrar el número de intento (nuevo campo
      `attemptNumber` o similar) para que el historial no se preste a confusión.
- [ ] Notificación solo se dispara tras agotar reintentos (no en cada intento
      fallido intermedio).
- [ ] Tests unitarios: 0 reintentos (comportamiento actual sin regresión),
      N reintentos con éxito en el intento k, N reintentos agotados.

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
