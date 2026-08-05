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

### 4. Observabilidad — `feature/observability-metrics`
- [x] `spring-boot-starter-actuator` ya estaba; se agregó `micrometer-registry-prometheus`.
- [x] `/actuator/prometheus` expuesto — **decisión**: Actuator corre en su
      propio puerto (`management.server.port`, default 9090 vía
      `MANAGEMENT_PORT`), separado de `server.port`. No se aseguró detrás del
      login de la app: se restringe por red (sin ingress público / Service de
      k8s solo intra-cluster), no con auth. `include: health,prometheus,info`.
- [x] Métricas custom:
      - Contador `chronos_cronjob_executions_total{project,result}` — por
        proyecto y resultado (success/failure), en `CronJobsServiceImpl`.
      - Timer `chronos_cronjob_execution_duration_seconds{project}`.
      - Gauge `chronos_cronjob_next_execution_timestamp_seconds{job,project}`
        vía `MultiGauge` en `CronJobScheduler`, recalculado en cada cambio de
        scheduling (no hay upsert por fila en `MultiGauge`, se recalcula el
        set completo — barato porque solo corre en altas/bajas/updates de
        jobs, no en cada tick).
      - Ejecuciones en modo Test (acción "Test") se excluyen de las métricas
        para no ensuciar success-rate/duración reales.
- [x] Tests: `CronJobTest` (nuevos casos para `getNextExecutionDateTime()`).
      El registro efectivo de métricas (`recordMetrics`) no tiene test unitario
      propio — depende de `crudService()`/contexto Spring como el resto de
      `CronJobsServiceImpl`; queda cubierto por verificación manual y, más
      adelante, por la suite de `feature/testcontainers-ci`.

### 5. Health probes — `feature/health-probes`
- [x] `management.endpoint.health.probes.enabled=true` → expone
      `/actuator/health/liveness` y `/actuator/health/readiness`.
- [x] Readiness ampliado más allá del default de Spring Boot
      (`readinessState` solo): `group.readiness.include: readinessState,db`.
      Decisión explícita — Chronos no puede cumplir su función (correr jobs
      HTTP y persistir el resultado) sin BD, así que una BD caída debe sacar
      el pod del Service de k8s, no solo marcarlo "started". El indicador
      `db` ya lo trae gratis `DataSourceHealthContributorAutoConfiguration`
      (query real de validación de conexión) — no hizo falta HealthIndicator
      custom.
- [x] Vive en el mismo puerto de management separado que
      `feature/observability-metrics` (mismo bloque `management:`, a
      reconciliar al integrar ambas ramas).
- [ ] Ejemplo de manifest k8s con estos probes: diferido a
      `feature/docker-deployment` (v1.5.0), donde vive el resto del manifest.

> **Nota:** esta feature es 100% configuración (`application.yml`), sin
> código Java. Verificar que readiness realmente cae cuando la BD no
> responde requiere un contexto Spring + Actuator + BD real levantados —
> no hay forma honesta de cubrirlo con un test unitario puro como los del
> resto del repo; queda como verificación manual (`curl` al endpoint con la
> BD caída) y, a futuro, como caso de la suite de `feature/testcontainers-ci`.

### 6. Executor configurable — `feature/executor-config`
- [x] `ChronosHttpExecutorProperties` (`chronos.http.thread-pool-size` /
      `chronos.http.connect-timeout`, override vía `CHRONOS_HTTP_THREAD_POOL_SIZE`/
      `CHRONOS_HTTP_CONNECT_TIMEOUT`) + `ChronosHttpClientConfig` que arma un
      `HttpClient` compartido sobre un `ExecutorService` de tamaño fijo.
      Reemplaza el `HttpClient.newHttpClient()` que se creaba nuevo (con su
      propio cached thread pool interno) en **cada** ejecución — antes no
      había ningún límite al crecimiento de threads bajo un "todos los jobs
      disparan a las :00".
- [x] `ChronosHttpRequestExecutor`/`CronJobHttpRequestExecutor` ganan un
      constructor de 4 args que recibe el `HttpClient`; el de 3 args
      (compatibilidad hacia atrás, usado por tests y por el envío manual de
      requests en `RequestItemViewModel`) usa un cliente default lazy,
      JVM-wide, no gestionado por Spring.
- [x] Confirmado: `serverTimeout` por job sigue gobernando el timeout de
      *request/response* (`HttpRequest.Builder.timeout(...)`) sin importar
      la config del cliente compartido — el `connectTimeout` global es una
      dimensión distinta (solo el TCP connect). Test
      `jobServerTimeoutTakesPriorityOverSharedClientConnectTimeout` lo prueba
      con un server que responde lento pero conecta instantáneo.

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
