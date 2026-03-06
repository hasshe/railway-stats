# 🚆 Movingo Tracker (railway-stats)

A self-hosted web app for tracking train punctuality on the **Uppsala C ↔ Stockholm C** corridor. It collects trip data nightly, stores it locally, and lets you browse historical delay/cancellation stats and claim eligibility. The **Metrics view** visualizes per-departure-time statistics as interactive Chart.js bar charts.

---

## Features

- **Automatic data collection:** Nightly job fetches all departures for both directions from the [TransitHub API](https://v2.api.transithub.se).
- **Rolling 30-day retention:** Oldest records are pruned to maintain a strict 30-day window.
- **Trip list:** Filterable cards show departure, arrival, minutes late, and status badges.
- **Claimable filter:** Shows only trips that were cancelled or ≥ 20 minutes late (Swedish reimbursement threshold).
- **Metrics view:** `/metrics` page with four bar charts: Average Minutes Late, Times Cancelled, Claims Requested, and Total Reimbursable Trips.
- **Departure-time filter:** Multi-select dropdown to filter charts by departure time.
- **Metrics FAB:** Floating button to access metrics from anywhere.
- **Profile drawer:** Save personal details for claims, encrypted client-side — including preferred **payout option** (SWISH or SUS).
- **Payout option selection:** Dropdown in the profile drawer to choose between SWISH and SUS; value is persisted encrypted in localStorage and sent with every claim submission.
- **Rate limiter:** IP-based protection against abuse.
- **Admin mode:** Password-protected, enables manual data collection and station management.
- **Global exception handling:** Typed exceptions (`StationNotFoundException`, `TripCollectionException`, `ClaimSubmissionException`, `ExternalApiException`) with clean, user-friendly notifications — no raw error messages exposed to the UI.

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
- Route selector, swap button, date picker, claimable filter, and admin controls.
- Scrollable trip card list.
- Metrics FAB (bottom-right).
- Profile drawer with all personal fields including a **Payout Option** dropdown (SWISH / SUS).

### Metrics view (`/metrics`)
- Back button, route selector, departure-time filter.
- Four stacked bar charts:
  - **Average Minutes Late** (amber)
  - **Times Cancelled** (red)
  - **Claims Requested** (green)
  - **Total Reimbursable Trips** (blue)

#### Chart details
- **Claims Requested:** Number of claims submitted per departure time (only real claims, not dev mode).
- **Total Reimbursable Trips:** Number of trips cancelled or ≥ 20 minutes late (updated automatically during nightly collection and metric refresh).

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

- Unlocks manual data collection, date clearing, and station management.
- Enable via Profile drawer (enter admin username, toggle, enter password).
- Session persists via encrypted localStorage.

---

## Scheduled Jobs

| Time   | Job                    | Description                                                      |
|--------|------------------------|------------------------------------------------------------------|
| 23:40  | Rolling-window pruning | Prunes oldest records to maintain 30-day window.                 |
| 23:50  | Trip data collection   | Fetches all departures, updates trip and metric tables.          |

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
- **Claims Requested** chart updates only for real claims (not dev mode).
- **Total Reimbursable Trips** chart updates automatically for qualifying trips.

---

## Project Structure

```
src/main/java/com/hs/railway_stats/
├── config/          # Station constants, GlobalExceptionHandler
├── dto/             # API request/response records, UserProfile
├── exception/       # Typed exceptions: StationNotFoundException, TripCollectionException, ClaimSubmissionException, ExternalApiException
├── external/        # TransitHub REST client (MalarDalenClient)
├── mapper/          # Maps API responses to internal DTOs
├── repository/      # JPA repositories + entities (TripInfo, TripInfoMetric, Translation)
├── service/
│   ├── TripInfoService / TripInfoServiceImpl          # Trip collection, retrieval, deletion, rolling-window pruning
│   ├── TripInfoMetricService / TripInfoMetricServiceImpl  # Metric upsert, query, departure times
│   ├── ClaimsService / ClaimsServiceImpl              # Claim submission
│   ├── TranslationService                             # Station name ↔ ID mapping
│   └── RateLimiterService                             # IP-based rate limiting
└── view/
    ├── TripInfoView.java     # Main view (route /)
    ├── MetricsView.java      # Metrics view (route /metrics)
    └── component/
        ├── TripStatsChart    # Vaadin wrapper for <trip-stats-chart> Chart.js web component
        ├── InputLayout       # Route selector (From/To labels + swap button) and date/filter controls
        ├── TripInfoCard      # Individual trip card + claim button
        ├── ProfileDrawer     # Slide-in profile + admin panel (incl. payout option dropdown)
        ├── AdminControls     # Collect / Add Station admin buttons
        ├── AdminBanner       # "Admin Mode Active" status banner
        ├── GitHubLink        # Header GitHub icon anchor
        └── ScheduledJobTimer # Next-run countdown display

src/main/frontend/
├── trip-stats-chart.js           # <trip-stats-chart> custom element (Chart.js)
├── icons/github.svg              # GitHub SVG icon
└── themes/railway-stats/
    ├── styles.css                # Stylesheet entry point (@import chain)
    ├── tokens.css                # CSS custom properties (colours, radii, shadows)
    ├── base.css                  # Reset, page shell, header grid, Lumo overrides
    ├── buttons.css               # All button variants (FAB, swap, admin, back, GitHub)
    ├── cards.css                 # Trip card list, badges, action button, empty state
    ├── input.css                 # Input form card, field labels, checkbox
    ├── chart.css                 # Chart element, route selector, departure filter
    └── profile-drawer.css        # Slide-in drawer: backdrop, panel, fields, footer
```

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
| `total_reimbursements_requested` | INTEGER | Claims submitted (shown in chart) |
| `canceled_trip_dates` | TEXT[] | Dates on which this departure was cancelled |

### `translation`
| Column | Type | Description |
|---|---|---|
| `id` | INTEGER (PK) | Auto-generated |
| `station_id` | INTEGER | TransitHub numeric station ID |
| `station_name` | TEXT | Human-readable display name |
| `claims_station_id` | TEXT | Station identifier for claim URLs |

---

## License

Personal use only. No license specified.

---

## Caching

- Trip info queries cached in-memory using [Caffeine](https://github.com/ben-manes/caffeine).
- Configurable via properties or YAML.
- Cache keys: origin, destination, date. Hits/misses logged. Empty results not cached. LRU eviction.
