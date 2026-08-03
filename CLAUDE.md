# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Dynamia Chronos: open source cron job scheduler / HTTP request runner built on Spring Boot + DynamiaTools (ZK-based admin UI framework). Lets users organize projects containing scheduled HTTP cron jobs, Postman-like request collections, variables, and multi-channel failure/execution notifications (email, SMS, Slack, generic webhook).

## Commands

- Build: `./mvnw compile`
- Run full test suite: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=ChronosHttpRequestExecutorTest`
- Run a single test method: `./mvnw test -Dtest=CronJobTest#headersRoundTripThroughJson`
- Run the app: `./mvnw spring-boot:run`
- Package: `./mvnw package`

### Database

The app requires a MySQL/Percona instance (`spring.datasource.url` in `application.yml`, defaults to `jdbc:mysql://localhost:3306/dynamiachronos`, overridable via `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD` env vars). `hibernate.ddl-auto: update` auto-migrates the schema on boot — there are no separate migration scripts. `DynamiaChronosApplicationTests` (`@SpringBootTest`) needs a live, reachable DB with correct credentials to pass; it will fail with a connection/auth error in environments without one configured. All other tests are pure JUnit and do not require a DB or Spring context.

## Architecture

### Request execution model

`ChronosHttpRequest` (`@MappedSuperclass`) is the shared base for anything that describes an outbound HTTP call: `serverHost`, `serverAuthorization`, `serverTimeout`, `requestBody`, `contentType`, `httpMethod`. Two entities extend it:
- `CronJob` — a scheduled job (adds `cronExpression`, `serverPort`, notification flags, execution stats, and `headers` as JSON-backed custom HTTP headers).
- `RequestItem` — a single request inside a `RequestCollection` (Postman-import style; adds `headers` and `parameters`, both JSON-backed maps).

`ChronosHttpRequestExecutor` takes any `ChronosHttpRequest` + a list of `Variable`s and runs it via `java.net.http.HttpClient`. It supports two optional mixins, checked with `instanceof`:
- `HeadersProvider` (`getHeaders(): Map<String,String>`) — extra headers merged in alongside `Authorization`/`Content-Type`. Implemented by `CronJob`, `RequestItem`, `RequestCollection`.
- `ParametersProvider` (`getParameters(): Map<String,String>`) — `:param` placeholders substituted in the URL. Implemented by `RequestItem`.

Both the URL and header values also go through `parseVariables`, a simple `{{variableName}}` → value substitution using the passed-in `Variable` list (project- or collection-scoped secrets/config).

`CronJobHttpRequestExecutor` extends the executor for scheduled jobs specifically: if `serverHost` doesn't start with `http`, it treats the job as a plain TCP reachability check (`Socket` connect to `serverHost:serverPort`, timing out per `serverTimeout`) instead of an HTTP call — used for monitoring non-HTTP services. Its `newResponse()` override produces a `CronJobLog` (persisted) instead of a transient `ChronosHttpResponse`.

When adding a new field that should behave like headers/parameters (JSON-backed map on an entity), follow the existing pattern: `@Column(columnDefinition = "json")` string field + `get`/`set` using `StringPojoParser.parseJsonToMap`/`convertMapToJson`, and make `set` null out the column when given `null`/empty (so the field can actually be cleared, not just added to).

### Scheduling

`CronJobScheduler` (`SchedulingConfigurer`) is the single source of truth for what's currently scheduled, keyed by `CronJob.id` in a `SimpleCache<Long, ScheduledTask>`. `CronJobCrudListener` and `ProjectCrudListener` (dynamiatools CRUD listeners) call `scheduler.scheduleJob(...)` after create/update/delete so the in-memory schedule always follows DB state — there's no polling.

`CronJobsServiceImpl.execute()` is the actual job runner (also used for the "Test" action via `test()`): builds a `CronJobHttpRequestExecutor`, persists a `CronJobLog`, updates `lastExecution`/`executionsCount`/`status` on the `CronJob`, and fires `CronJobExecutionListener` before/after hooks. `CronJobExecutionNotificationSenderListener` is the built-in listener that fans out to all active `Notificator`s for the job's project via `NotificationSender` (one `@Provider` bean per channel: `email`, `sms`, `slack`, `webhook`, matched by `Notificator.sender` id).

### UI layer (ZK / dynamiatools)

Pages are ZUL files in `src/main/resources/zk/pages/`, each paired with an `AbstractViewModel<T>`-based ZK MVVM class in `vm/`. `ChronosModuleProvider` registers the module/pages into dynamiatools' navigation. `AbstractProjectsViewModel` builds the shared project/cron-jobs/collections tree (`EntityTreeNode`) used by both the dashboard and the projects page; `DashboardViewModel` and `ProjectsViewModel` extend it for their respective layouts.

Entities/collections that need a header- or key-value-style editor in the UI don't have a generic dynamiatools widget for it — the established pattern (see `RequestItemViewModel`/`request.zul`, `CronJobViewModel`/`cronjob.zul`) is: keep a `List<LabelValue>` in the view model as the editable UI model, convert to/from the entity's `Map<String,String>` getter/setter on load/save, and bind it to a `listbox` with inline-editable `textbox`es plus add/remove commands.

Form field layout for simple entities is declarative via YAML descriptors in `src/main/resources/META-INF/descriptors/` (e.g. `CronJobForm.yml`), consumed through `<viewer viewType="form" beanClass="..."/>` or `<viewer descriptorId="..."/>` in ZUL. Entities needing custom tabs/behavior (headers editors, test/send actions) wrap that generic form viewer inside a hand-written ZUL page instead of relying purely on the descriptor.

### Notifications

`NotificationSender` implementations are `@Provider` beans looked up via `Containers.get().findObjects(...)`/`findObject(...)` (dynamiatools' service locator, not plain `@Autowired` injection) by matching `Notificator.getSender()` against `NotificationSender.getId()`. When adding a new channel, implement `NotificationSender`, annotate `@Provider`, and pick a unique `getId()`.
