<div align="center">

# ⏱️ Dynamia Chronos

**Never babysit a cron tab again.**

Open source cron job scheduler and HTTP request runner with a full admin UI — schedule requests, monitor failures, and get notified on Slack, email, SMS or webhook the moment something breaks.

Built with Spring Boot and [Dynamia Tools](https://www.dynamiasoluciones.com).

[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-brightgreen.svg)](https://spring.io/projects/spring-boot)

</div>

---

## Why Chronos?

Cron jobs that call an HTTP endpoint usually end up scattered across `crontab -e` entries, `curl` scripts, and tribal knowledge of "who set that up." Chronos gives you a proper home for them:

- 🗂️ **Organize by project** — group cron jobs, request collections, variables and notification channels per team or product.
- ⏰ **Schedule anything HTTP** — GET/POST/PUT/DELETE/HEAD/OPTIONS, standard cron expressions, custom headers, auth, timeouts, and body.
- 🔌 **Monitor plain TCP services too** — no HTTP endpoint? Point a job at `host:port` and Chronos checks reachability instead.
- 🧰 **Postman-style request collections** — import an existing Postman export and reuse it as a manual API workbench, no separate tool needed.
- 🧩 **Reusable variables** — `{{secrets}}`/`{{config}}` shared across jobs and requests, scoped per project or collection.
- 📜 **Full execution history** — every run logs status, duration, response size, and body, whether triggered by the scheduler or a manual "Test" click.
- 🔔 **Notify where your team already is** — email, SMS, Slack webhook, or a generic HTTP webhook, on failure and/or success.
- 🖥️ **Batteries-included admin UI** — a complete CRUD web app out of the box, no separate frontend to build or deploy.

## Requirements

- Java 25
- Maven (or use the bundled `./mvnw`)
- MySQL / Percona (schema is auto-migrated on boot via `hibernate.ddl-auto: update`)

## Quick start

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

## Running with Docker

Chronos uses Spring Boot's built-in [Cloud Native Buildpacks](https://spring.io/guides/topicals/spring-boot-docker/) support — no `Dockerfile` needed. Requires a running Docker daemon.

Build the image:

```bash
./mvnw spring-boot:build-image
```

This produces `docker.io/library/dynamia-chronos:1.1.0` (name and tag follow `artifactId:version` by default). To use a custom name/tag instead:

```bash
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=yourrepo/dynamia-chronos:latest
```

Run it, pointing at your database:

```bash
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:mysql://host.docker.internal:3306/dynamiachronos \
  -e DATABASE_USERNAME=root \
  -e DATABASE_PASSWORD=yourpassword \
  dynamia-chronos:1.1.0
```

> On Linux, replace `host.docker.internal` with your host's IP (or run MySQL in the same Docker network) since that hostname is a Docker Desktop convenience for Mac/Windows.

Push it to your registry like any other image:

```bash
docker push yourrepo/dynamia-chronos:latest
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

## Contributing

Issues and pull requests are welcome. If you're proposing a larger change, open an issue first so we can align on the approach.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
