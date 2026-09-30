# Premier League Analytics

Event-driven microservices platform for ingesting, tracking, and serving
Premier League match data in near real time.

## Modules

| Module | What it does |
|---|---|
| `pl-core-data` | Shared library — JPA entities, DTOs, repositories (Postgres). Not a runnable service. |
| `data-ingestion-service` | Scrapes match/season/standings data (Playwright) and loads it into Postgres. |
| `live-match-tracker` | Polls live match data on a schedule and publishes events to Kafka. (port 4000) |
| `premier-league-service` | Kafka consumer + REST API layer. (port 4002) |

## Running it locally (Docker)

1. Copy the env template and fill in real values:
   ```
   cp .env.example .env
   ```
   (`.env` is git-ignored — your real DB credentials never leave your machine.)

2. Build and start everything:
   ```
   docker compose up --build
   ```
   This brings up Postgres, Zookeeper, Kafka, and all three services.

3. Check it's alive:
   - `premier-league-service` Swagger UI: http://localhost:4002/swagger-ui.html
   - `live-match-tracker`: http://localhost:4000

4. Tear down:
   ```
   docker compose down          # keep data
   docker compose down -v       # wipe the Postgres volume too
   ```

## Running a single module without Docker

Each module is a normal Maven project. Since `pl-core-data` is a dependency
of the other three, install it first:

```
cd pl-core-data && mvn install
cd ../premier-league-service && mvn spring-boot:run
```

You'll need Postgres (and Kafka, for `live-match-tracker` /
`premier-league-service`) running locally, and these env vars set:

```
export DB_USERNAME=...
export DB_PASSWORD=...
```

## Changes made in this pass

- **Removed hardcoded DB credentials** from all four `application.properties`
  files — they now read `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` from the
  environment, with `.env` (git-ignored) holding the real local values and
  `.env.example` committed as a safe placeholder template.
- **Added `.gitignore`** (`.env`, `target/`, IDE files).
- **Fixed three invalid Maven artifact IDs** in `live-match-tracker/pom.xml`
  (`spring-boot-starter-webmvc` → `spring-boot-starter-web`; removed
  `spring-boot-starter-data-jpa-test`, `spring-boot-starter-validation-test`,
  `spring-boot-starter-webmvc-test`, and `spring-boot-h2console` — none of
  these are real published artifacts and would have failed dependency
  resolution) and one in `premier-league-service/pom.xml`
  (`spring-boot-persistence`, also not a real artifact).
- **Added a `Dockerfile` per service** and a root **`docker-compose.yml`**
  wiring up Postgres + Kafka + Zookeeper + all three services.

### A note on verification

I wrote and reviewed all of the above carefully, but I was not able to
actually **run** `mvn install`, `docker compose up`, or any build in the
sandbox I did this work in — it has no Docker daemon and no network route to
Maven Central, so dependency resolution isn't possible there. Everything
above should be correct, but please treat the first `docker compose up
--build` as the real test, and let me know what errors (if any) come up —
I'll fix them. The one part I'd watch most closely is the Playwright browser
install step in `data-ingestion-service/Dockerfile`; it's the trickiest
piece to get right without being able to test it myself.
