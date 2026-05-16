# 🚆 Movingo Tracker (railway-stats)

A self-hosted web app for tracking train punctuality on the **Uppsala C ↔ Stockholm C** corridor. It collects trip data nightly, stores it locally, and lets you browse historical delay/cancellation stats and claim eligibility. The **Metrics view** visualizes per-departure-time statistics as an interactive, consolidated Chart.js bar chart with a legend.

> **Current version: 3.4.1**

---

## Features

- **Automatic data collection:** Nightly job fetches all departures for both directions from the [TransitHub API](https://v2.api.transithub.se).
- **Retry on collection failure:** The nightly collection job automatically retries on failure with configurable exponential backoff (see `tripinfo.retry.*`).
- **Rolling 30-day retention:** Oldest records are pruned to maintain a strict 30-day window (configurable via `tripinfo.retention.days`).
- **Trip list:** Filterable cards show departure, arrival, minutes late, and status badges.
- **Trip filter dropdown:** A multi-option dropdown replaces the old claimable checkbox, offering five filter modes — see [Trip Filter](#trip-filter) below.
- **Smart date picker:** Automatically defaults to the latest gathered date, so you see the most recent data immediately upon page load.
- **Metrics view:** `/metrics` page with a unified, interactive bar chart displaying three metrics (Times Cancelled, Claims Requested, Total Reimbursable Trips) with a color-coded legend for easy comparison.
- **Departure-time filter:** Multi-select dropdown to filter charts by departure time.
- **Metrics FAB:** Floating button to access metrics from anywhere.
- **Profile drawer:** Save personal details for claims, encrypted client-side — including preferred **payout option** (SWISH or SUS).
- **Payout option selection:** Dropdown in the profile drawer to choose between SWISH and SUS; value is persisted encrypted in localStorage and sent with every claim submission.
- **Rate limiter:** IP-based protection against abuse.
- **Admin mode:** Password-protected, enables manual data collection, date clearing, cache management, and station management. The full date picker is available in admin mode (no `today - 1` restriction).
- **Global exception handling:** Typed exceptions (`StationNotFoundException`, `TripCollectionException`, `ClaimSubmissionException`, `ExternalApiException`) with clean, user-friendly notifications — no raw error messages exposed to the UI.
- **Dev mode:** Claim submissions are intercepted at the UI layer in dev mode and never reach the service layer — metrics tables are **not** updated for dev claims.

---

## Tech Stack

- **Java 21** / **Spring Boot 4.x**
- **Vaadin 25** (UI)
- **Chart.js 4.4** (metrics charts)
- **Spring Data JPA** + **H2** (dev) / **PostgreSQL** (prod)
- **Maven 3.9**, **Lombok**, **Jackson**

---

## Views

### Main view (`/`)
- Header with profile/menu, title, and GitHub link.
- Route selector, swap button, date picker, **trip filter dropdown**, and admin controls.
- Scrollable trip card list.
- Metrics FAB (bottom-right).
- Profile drawer with all personal fields including a **Payout Option** dropdown (SWISH / SUS).

### Metrics view (`/metrics`)
- Back button, route selector, departure-time filter.
- **Unified metrics chart** with interactive legend:
  - **Times Cancelled** (red) — Number of cancellations per departure time
  - **Claims Requested** (green) — Number of claims submitted per departure time
  - **Total Reimbursable Trips** (blue) — Trips cancelled or ≥ 20 min late per departure time
- Legend is positioned at the top and allows toggling individual metrics on/off
- Single consolidated view reduces scrolling and enables easy side-by-side metric comparison

#### Chart details
- **Claims Requested:** Number of claims submitted per departure time (only real claims, not dev mode).
- **Total Reimbursable Trips:** Number of trips cancelled or ≥ 20 minutes late (updated automatically during nightly collection and metric refresh).
- Metrics queries are cached in-memory using Caffeine for fast chart rendering. The cache is cleared and repopulated nightly at 23:59.

---

## Date Picker

The date picker on the main view automatically defaults to the latest gathered date, ensuring you see the most recent data immediately:

- **Smart Default:** On page load, the date picker is set to the latest date with collected trip data
  - Example: If today is May 16th and data was collected for May 15th, the picker shows May 15th
  - No manual navigation needed to find recent data
- **Fallback Behavior:** If no data exists in the database, defaults to yesterday (LocalDate.now().minusDays(1))
- **Admin Override:** In admin mode, the picker can go back to any available date (no `today - 1` restriction)
- **Timezone Aware:** All date calculations use Stockholm timezone for consistency

---

## Trip Filter

The trip list filter is a dropdown (`TripFilter` enum, `TripInfoCard`) that replaces the old **Claimable** checkbox. It defaults to **Claimable (All)** on load.

| Option | Behaviour |
|---|---|
| **Claimable (All)** *(default)* | Trips that are cancelled **or** ≥ 20 min late |
| **Late (>= 20min)** | Trips that are ≥ 20 min late and not cancelled |
| **Late (All)** | Any trip with a delay > 0 min (not cancelled) |
| **Cancelled** | Only cancelled trips |
| **No Filter** | All trips, unfiltered |

---

## Payout Option

The profile drawer includes a **Payout Option** dropdown with two choices:

| Option | Description |
|--------|-------------|
| `SWISH` | Reimbursement paid via Swish (default) |
| `SUS`   | Reimbursement paid via Swedbank SUS |

- The selected value is saved encrypted in `localStorage` alongside other profile fields.
- It is loaded automatically when the drawer opens and pre-populated from storage.
- The value is sent as the `payoutOption` field in every claim request.
- Defaults to `SWISH` if no value has been saved.

---

## Exception Handling

All service-layer errors are represented by typed exceptions that map to specific HTTP status codes and clean user-facing messages. Raw error details are never shown in the UI.

| Exception | Thrown when | HTTP status | User message |
|---|---|---|---|
| `StationNotFoundException` | Station name/ID not found in DB | 404 Not Found | "Station not found. Please check the selected route and try again." |
| `TripCollectionException` | External trip API fetch fails | 503 Service Unavailable | "Could not collect trip data. Please try again later." |
| `ClaimSubmissionException` (rate-limited) | API returns 422 | 429 Too Many Requests | "You have too many pending claims. Please wait a moment before trying again." |
| `ClaimSubmissionException` | Other claim API failure | 502 Bad Gateway | "Claim submission failed. Please try again later." |
| `ExternalApiException` | Non-2xx from TransitHub API | 502 Bad Gateway | "An external service is currently unavailable. Please try again later." |
| `Exception` (fallback) | Any unhandled error | 500 Internal Server Error | "An unexpected error occurred. Please try again." |

The `GlobalExceptionHandler` (`@ControllerAdvice`) centralises all handling, logs errors with appropriate severity, and returns a structured `ErrorResponse` with `userMessage` and `details` fields.

---

## Running Locally

- Requires Java 21+ and Maven 3.9+
- Run: `./mvnw spring-boot:run`
- App: [http://localhost:8080](http://localhost:8080)
- Embedded H2 DB: inspect at `/h2-console` (JDBC URL: `jdbc:h2:file:./data/trip_info`, user: `sa`, no password)

---

## Production Deployment

- Set environment variables for DB, crypto, admin, and rate limiter settings.
- Build: `./mvnw -Pproduction package -DskipTests`
- Run: `SPRING_PROFILES_ACTIVE=prod ... java -jar target/railway-stats-*.jar`

---

## Admin Mode

- Unlocks manual data collection, date clearing, cache management, and station management.
- Enable via the **Admin Mode** accordion in the Profile drawer — enter username and password, then click **Login as Admin**.
- The accordion is always visible in the drawer and starts collapsed.
- Session persists via encrypted localStorage. Admin mode is restored automatically on page reload.
- Disabling admin mode (via the same login fields) immediately reverts all admin-only UI changes, including the date picker restriction, without requiring a page refresh.
- Admin panel includes buttons to clear the Trip Info cache, Metrics cache, and Translation cache instantly.

---

## Scheduled Jobs

All cron expressions and the retention window are configured in `application.yml` under `tripinfo.scheduling` and `tripinfo.retention`.

| Time  | Property key                              | Job                    | Description                                               |
|-------|-------------------------------------------|------------------------|-----------------------------------------------------------|
| 23:40 | `tripinfo.scheduling.prune-cron`          | Rolling-window pruning | Prunes oldest records to maintain the retention window.   |
| 23:50 | `tripinfo.scheduling.collect-cron`        | Trip data collection   | Fetches all departures, updates trip and metric tables. Retries on failure with exponential backoff. |
| 23:59 | `tripinfo.scheduling.metrics-refresh-cron`| Metrics cache refresh  | Clears and repopulates metrics cache for all station pairs.|

All scheduled jobs run in `TripCollectionScheduler`. Pruning logic lives in `TripPruningService`. Retry behaviour on the collection job is implemented via `@Retryable` on `TripInfoService.collectTripInformation` and is fully configurable via the `tripinfo.retry.*` properties.

---

## Personal Data & Encryption

- No personal data sent to server.
- Profile details (name, phone, email, address, postal code, ticket number, identity number, **payout option**) are encrypted in browser using AES-GCM and PBKDF2.
- Delete data by clearing `userProfile` from localStorage.

---

## Claim Button & Trip Tracking

- Claim button shown for eligible trips (cancelled or ≥ 20 min late).
- Claim marks trip as claimed in localStorage; button replaced with label.
- Cannot claim same trip twice from same browser.
- **Payout option** from profile (SWISH or SUS) is included in the claim request; defaults to SWISH if not set.
- **Claims Requested** chart updates only for real claims — dev mode intercepts submissions at the UI layer before they reach the service or metrics layer.
- **Total Reimbursable Trips** chart updates automatically for qualifying trips during nightly collection.

---

## Configuration Reference

Key properties across `application.yml`, `application-dev.yml`, and `application-prod.yml`:

| Property | Default | Description |
|---|---|---|
| `app.dev-mode` | `false` | Enables dev mode (claim submissions intercepted in UI) |
| `tripinfo.scheduling.prune-cron` | `0 40 23 * * ?` | Cron for rolling-window pruning |
| `tripinfo.scheduling.collect-cron` | `59 50 23 * * ?` | Cron for nightly trip collection |
| `tripinfo.scheduling.metrics-refresh-cron` | `0 59 23 * * ?` | Cron for metrics cache refresh |
| `tripinfo.scheduling.zone` | `Europe/Stockholm` | Timezone for all scheduled jobs |
| `tripinfo.retention.days` | `30` | Number of days of trip data to retain |
| `tripinfo.retry.max-attempts` | `3` | Total collection attempts (1 initial + retries) |
| `tripinfo.retry.initial-interval-ms` | `5000` | Initial backoff delay in milliseconds |
| `tripinfo.retry.multiplier` | `2.0` | Exponential backoff multiplier between retries |
| `tripinfo.retry.max-interval-ms` | `60000` | Maximum backoff delay cap in milliseconds |
| `tripinfo.cache.expiry.hours` | `24` | Trip info cache TTL |
| `tripinfo.cache.max-size` | `100` | Trip info cache max entries |
| `metrics.cache.max-size` | `50` | Metrics cache max entries |
| `external.api.trip-search-url` | `https://bff.malardalstrafik.se/v2/trip` | TransitHub trip search endpoint |
| `external.api.claims-url` | `https://evf-regionsormland.preciocloudapp.net/api/Claims` | Claims submission endpoint |

---

## Database Schema

### `trip_info`
| Column | Type | Description |
|---|---|---|
| `id` | INTEGER (PK) | Auto-generated |
| `origin_id` | INTEGER | TransitHub station ID for origin |
| `destination_id` | INTEGER | TransitHub station ID for destination |
| `original_departure_time` | TIMESTAMPTZ | Scheduled departure |
| `actual_arrival_time` | TIMESTAMPTZ | Actual arrival |
| `canceled` | INTEGER | `1` = cancelled, `0` = not cancelled |
| `minutes_late` | INTEGER | Minutes behind schedule |
| `created_at` | TIMESTAMPTZ | Record creation timestamp |

### `trip_info_metric`
| Column | Type | Description |
|---|---|---|
| `id` | INTEGER (PK) | Auto-generated |
| `origin_id` | INTEGER | TransitHub station ID for origin |
| `destination_id` | INTEGER | TransitHub station ID for destination |
| `scheduled_departure_time` | TIME | Scheduled departure time (HH:mm) |
| `average_minutes_late` | INTEGER | Rolling average minutes late |
| `total_trips` | INTEGER | Total trips recorded for this slot |
| `total_reimbursable_trips` | INTEGER | Trips cancelled or ≥ 20 min late (shown in chart) |
| `total_reimbursements_requested` | INTEGER | Claims submitted — incremented atomically via a single `UPDATE` query |
| `canceled_trip_dates` | TEXT[] | Dates on which this departure was cancelled |

### `translation`
| Column | Type | Description |
|---|---|---|
| `id` | INTEGER (PK) | Auto-generated |
| `station_id` | INTEGER | TransitHub numeric station ID |
| `station_name` | TEXT | Human-readable display name (stored lowercase) |
| `claims_station_id` | TEXT | Station identifier for claim URLs |

---

## License

Personal use only. No license specified.

---

## Caching

- Trip info and metrics queries cached in-memory using [Caffeine](https://github.com/ben-manes/caffeine).
- Metrics cache is cleared and repopulated nightly at 23:59 for all station pairs via `TripInfoMetricService.refreshMetricsCache`.
- **Admin users can clear all three caches (Trip Info, Metrics, Translation) instantly from the admin panel.**
- Configurable via YAML:
  - `tripinfo.cache.expiry.hours`, `tripinfo.cache.max-size`
  - `metrics.cache.max-size`
- Cache keys:
  - Trip info: `origin-destination-date`
  - Metrics: `origin-destination`
  - Translation: station name (lowercase), station ID, or claims station ID — all three keys populated on first load
- Hits/misses logged at DEBUG level. Empty results not cached. LRU eviction.
