# Dynamia Chronos

Open source cron job scheduler and HTTP request runner, built with Spring Boot and [Dynamia Tools](https://www.dynamiasoluciones.com).

Chronos lets you organize **Projects** containing scheduled HTTP cron jobs, Postman-like request collections, shared variables, and multi-channel notifications (email, SMS, Slack, generic webhook) when a job fails or executes.

## Features

- **Cron jobs**: schedule recurring HTTP requests (GET/POST/PUT/DELETE/HEAD/OPTIONS) using standard cron expressions. Configure URL, method, body, content type, auth header, timeout, and custom HTTP headers per job.
- **TCP reachability checks**: point a job's host at a non-HTTP address to monitor a service via a plain TCP connection instead of an HTTP call.
- **Request collections**: Postman-style folders of requests with their own headers/parameters/variables, importable directly from a Postman export.
- **Variables**: project- or collection-scoped `{{variable}}` substitution available in URLs and headers, useful for secrets/config that shouldn't be hardcoded per request.
- **Execution logs**: every run (scheduled or manual "Test") is persisted with status, duration, response body/headers and size.
- **Notifications**: notify on failure and/or execution via email, SMS, Slack webhook, or a generic HTTP webhook.
- **Admin UI**: full CRUD web UI built on ZK via Dynamia Tools, no separate frontend project required.

## Requirements

- Java 25
- Maven (or use the bundled `./mvnw`)
- MySQL / Percona (schema is auto-migrated on boot via `hibernate.ddl-auto: update`)

## Running locally

```bash
./mvnw spring-boot:run
```

By default the app connects to `jdbc:mysql://localhost:3306/dynamiachronos` with user `root`. Override via environment variables:

```bash
DATABASE_URL=jdbc:mysql://localhost:3306/dynamiachronos \
DATABASE_USERNAME=root \
DATABASE_PASSWORD=yourpassword \
PORT=8080 \
./mvnw spring-boot:run
```

Once started, the admin UI is available at `http://localhost:8080`.

## Building

```bash
./mvnw clean package
```

## Testing

```bash
./mvnw test
```

Run a single test class or method:

```bash
./mvnw test -Dtest=ChronosHttpRequestExecutorTest
./mvnw test -Dtest=CronJobTest#headersRoundTripThroughJson
```

> `DynamiaChronosApplicationTests` boots the full Spring context and requires a reachable MySQL database; the rest of the suite is plain JUnit and has no external dependencies.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
