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
   - `premier-league-service` dashboard: http://localhost:4002 (standings, matches,
     team of the week, top performers, AI match summaries)
   - `premier-league-service` Swagger UI: http://localhost:4002/swagger-ui.html
   - `live-match-tracker`: http://localhost:4000/api/live_match_ingestion/status
     (no UI — it's a Kafka-polling job service; see its endpoints in
     `IngestionController`)

4. Load some sample data to explore the dashboard with (the real scraper needs
   network access to the source site and can get blocked by bot detection):
   ```
   docker exec -i premierleague-postgres-1 psql -U "$DB_USERNAME" -d premier_league_db < scripts/sample-data.sql
   ```
   This seeds one round of fictional matches (6 teams, full lineups, stats,
   goals, standings) — safe to re-run.

5. Tear down:
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

## AI match summaries

`premier-league-service` has a `GET /api/premier-league/matches/{matchId}/summary`
endpoint that generates a natural-language match recap from the stored score,
team stats, incidents, and top-rated players, using Google's Gemini API.

1. Get a free-tier API key from https://aistudio.google.com/apikey (no
   billing account needed).
2. Set `GEMINI_API_KEY` in `.env`.
3. Try it from the dashboard's "AI Match Summary" tab, or directly:
   ```
   curl http://localhost:4002/api/premier-league/matches/{matchId}/summary
   ```

Leave `GEMINI_API_KEY` blank to disable the feature (it returns a 503 with
a clear message instead of failing to start).

## Sample data

`scripts/sample-data.sql` seeds one round of fictional Premier League
matches — 6 teams, full lineups with ratings, team/player stats, goal
incidents, and standings — so the dashboard and API have something to show
without needing the live scraper to succeed. See step 4 above for how to
load it.

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
- **Bumped Playwright 1.40.0 → 1.63.0** in `data-ingestion-service`: 1.40's
  `--with-deps` installer doesn't know Ubuntu 24.04 (noble) package names.
- **Switched `ddl-auto` from `validate` to `update`** in the three runnable
  services — there's no migration tool, so `validate` failed against an
  empty database.
- **Fixed a SQL injection** in `GET /home/topPerformers`: the `column` query
  param was interpolated directly into raw SQL with no validation. Now
  whitelisted in the controller.
- **Added the AI match-summary endpoint** (Gemini) and a **static dashboard**
  (`premier-league-service/src/main/resources/static/`) as a simple
  alternative to Swagger for exploring the API.

All of the above has been built, run, and exercised end-to-end via
`docker compose up --build` — not just reviewed on paper.
