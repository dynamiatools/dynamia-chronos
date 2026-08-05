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

### 7. CI real con Testcontainers — `feature/testcontainers-ci`
- [x] Testcontainers MySQL introducido: `spring-boot-testcontainers` +
      `org.testcontainers:junit-jupiter`/`mysql`. **Nota de versión**: el
      `testcontainers.version` que trae el parent de Spring Boot (2.0.5) no
      tiene publicados los módulos `junit-jupiter`/`mysql` todavía — se
      pineó la property a `1.21.4` (la última que sí los publica) en el
      `<properties>` del propio pom, no por dependencia individual, para que
      `testcontainers-core` y los módulos queden en la misma versión. Revisar
      al hacer bump de dynamiatools/Spring Boot si 2.x ya los publicó.
- [x] `DynamiaChronosApplicationTests` ahora usa `@Testcontainers` +
      `@Container` + `@ServiceConnection` sobre un `MySQLContainer` — ya no
      depende de `DATABASE_URL`/una BD externa alcanzable, solo de un daemon
      Docker. Verificado corriendo localmente: bootea el contexto completo
      contra MySQL real (~16-40s con el pull de la imagen).
- [x] GitHub Actions (`.github/workflows/ci.yml`): `./mvnw test` en cada push
      a `main` y en cada PR, sobre `ubuntu-latest` (Docker viene preinstalado,
      no hace falta provisionar nada aparte).
- [x] `CLAUDE.md` actualizado: la nota de "necesita BD externa reachable" para
      `DynamiaChronosApplicationTests` ya no aplica.

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
