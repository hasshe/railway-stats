# 🚆 Movingo Tracker (railway-stats)

A self-hosted web application for tracking train punctuality on the **Uppsala C ↔ Stockholm C** corridor. It automatically collects trip data every night, stores it locally, and lets you browse historical delay and cancellation stats — including which journeys qualify for a reimbursement claim. A dedicated **Metrics view** visualises per-departure-time statistics as interactive Chart.js bar charts.

---

## What it does

| Feature | Description |
|---|---|
| **Automatic data collection** | A scheduled job runs every night at **23:50 (Europe/Stockholm)** and fetches all departures for both directions (Uppsala → Stockholm and Stockholm → Uppsala) from the [TransitHub API](https://v2.api.transithub.se). |
| **Rolling 30-day retention** | A second scheduled job runs at **23:40 (Europe/Stockholm)**, before data collection, and automatically removes the oldest day's records once the table holds more than 30 days of data — ensuring a strict rolling window is maintained. |
| **Trip list** | The main view shows a filterable list of trip cards for any past date, each showing departure time, arrival time, minutes late, and status badges. |
| **Claimable filter** | A "Claimable" checkbox filters the list to only show trips that were **cancelled** or **≥ 20 minutes late** — the Swedish threshold for a reimbursement claim. |
| **Swap button** | Quickly swap origin and destination to view the return leg. |
| **Metrics view** | A separate `/metrics` page with three independent Chart.js bar charts: **Average Minutes Late**, **Times Cancelled**, and **Claims Requested** — one per scheduled departure time. |
| **Departure-time filter** | A multi-select dropdown on the Metrics page lets you filter charts to specific departure times. |
| **Metrics FAB** | A sticky floating action button (bottom-right corner) on the main view navigates to the Metrics page from anywhere on the page. |
| **Profile drawer** | Optionally save your personal details (name, address, ticket number, etc.) in the browser for convenience when filing claims. All data is encrypted client-side. |
| **Rate limiter** | IP-based rate limiter (20 requests / 5 minutes, 15-minute block) protects the API endpoint from abuse. |
| **Admin mode** | Password-protected admin mode unlocked via the Profile drawer. Persists across page refreshes using encrypted `localStorage`. |
| **Collect (Admin)** | Manually triggers the trip data collection job on demand without waiting for the nightly scheduler. |
| **Add Station (Admin)** | Adds a new station (TransitHub station ID + display name) to the translation table directly from the UI. |

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | **Java 21** |
| Framework | **Spring Boot 4.x** |
| UI | **Vaadin 25** (server-side, with custom web components) |
| Charts | **Chart.js 4.4** (via a `<trip-stats-chart>` custom element) |
| Persistence | **Spring Data JPA** + **H2** (dev) / **PostgreSQL** (prod) |
| Build | **Maven 3.9**, **Lombok**, **Jackson** |

---

## Views

### Main view (`/`)

The landing page. Contains:
- A **header row** with the profile/menu button, "Movingo Tracker" title, and GitHub link.
- A **route selector** showing the current origin → destination as styled read-only labels (matching the Metrics view), with a swap button. Below it, a date picker, claimable filter, and admin controls sit in a form card.
- A scrollable **trip card list** filtered by the selected route and date.
- A sticky **Metrics FAB** (green circle, bottom-right) that navigates to `/metrics`.

### Metrics view (`/metrics`)

Accessible via the FAB or directly at `/metrics`. Contains:
- A **back button** returning to the main view.
- A **route selector** showing the current origin → destination as read-only labels with a swap button (no free-text input).
- A **departure-time filter** — a multi-select combo box populated with all distinct scheduled departure times for the selected route. Selecting one or more times restricts all three charts to those departures. Clearing the selection shows all.
- Three stacked **Chart.js bar charts**, one each for:
  - **Average Minutes Late** (amber)
  - **Times Cancelled** (red)
  - **Claims Requested** (green)

---

## Running locally (dev)

### Prerequisites
- Java 21+
- Maven 3.9+

### Steps

```bash
# Clone the repository
git clone https://github.com/hasshe/railway-stats.git
cd railway-stats

# Run with the dev profile (H2 in-file database, no env vars needed)
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080** and the browser opens automatically.

The dev profile uses an embedded H2 database stored in `./data/trip_info.mv.db`.  
You can inspect the database at **http://localhost:8080/h2-console** with:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/trip_info` |
| Username | `sa` |
| Password | *(leave blank)* |

### Dev defaults (application-dev.yml)

| Setting | Value |
|---|---|
| Crypto secret | `railway-stats-key-v1` |
| Crypto salt | `railway-stats-salt` |
| Admin password | `admin123` |
| Admin username | `Admin` |
| Rate limiter max requests | `20` |
| Rate limiter window | `300` s (5 min) |
| Rate limiter block duration | `900` s (15 min) |

---

## Running tests

```bash
./mvnw test
```

Test reports are written to `target/surefire-reports/`.

---

## Production deployment

The prod profile expects the following **environment variables**:

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL, e.g. `jdbc:postgresql://host:5432/railway` |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `CRYPTO_SECRET` | Secret key used to encrypt the browser profile (change from dev default!) |
| `CRYPTO_SALT` | Salt used in PBKDF2 key derivation (change from dev default!) |
| `ADMIN_PASSWORD` | Password for the admin panel |
| `ADMIN_USERNAME` | First name value that reveals the admin toggle in the Profile drawer |
| `RATE_LIMITER_MAX_REQUESTS` | *(optional)* Max requests per IP per window — default `20` |
| `RATE_LIMITER_WINDOW_SECONDS` | *(optional)* Sliding window size in seconds — default `300` (5 min) |
| `RATE_LIMITER_TIMEOUT_SECONDS` | *(optional)* Block duration in seconds after limit exceeded — default `900` (15 min) |

Activate the prod profile by setting `spring.profiles.active=prod` (or via `SPRING_PROFILES_ACTIVE=prod`).

```bash
# Build a fat JAR
./mvnw -Pproduction package -DskipTests

# Run
SPRING_PROFILES_ACTIVE=prod \
  DB_URL=jdbc:postgresql://localhost:5432/railway \
  DB_USERNAME=postgres \
  DB_PASSWORD=secret \
  CRYPTO_SECRET=my-strong-secret \
  CRYPTO_SALT=my-unique-salt \
  ADMIN_PASSWORD=changeme \
  ADMIN_USERNAME=YourName \
  java -jar target/railway-stats-*.jar
```

---

## Admin mode

Admin mode unlocks three extra controls in the toolbar: **Collect (Admin)**, **Clear Date (Admin)**, and **Add Station (Admin)**.

### Enabling / disabling

1. Open the **Profile drawer** (hamburger menu, top-left).
2. Type the configured `ADMIN_USERNAME` value into the **First Name** field — the **Toggle Admin Mode** button appears.
3. Click **Toggle Admin Mode** and enter the `ADMIN_PASSWORD`.
4. On success the 🔐 **Admin Mode Active** banner appears and all three admin buttons are shown.
5. Clicking **Toggle Admin Mode** again (with the correct password) disables admin mode.

Admin mode is persisted across page refreshes via an encrypted `localStorage` entry (`adminSession`). The session is cleared on explicit toggle-off.


### Collect (Admin)

Manually triggers the same trip-data collection job that normally runs on the nightly schedule (every day at **23:50 Europe/Stockholm**). Useful after adding a new station or to back-fill data without restarting the server.

### Clear Date (Admin)

Deletes **all trip records** for the date currently selected in the date picker, then refreshes the trip list. Useful for removing bad or duplicate data for a specific day so it can be re-collected cleanly via **Collect (Admin)**.

### Add Station (Admin)

Opens a dialog to register a new station in the translation table:

| Field | Description |
|---|---|
| **Station ID** | The numeric TransitHub station ID (e.g. `74100` for Arlanda C) |
| **Station Name** | Human-readable display name shown in the UI |

Duplicate station IDs and duplicate station names are both rejected with an error notification.

---

## Scheduled jobs

Two jobs run automatically every night in `Europe/Stockholm` time:

| Time | Job | Description |
|---|---|---|
| **23:40** | Rolling-window pruning | Deletes the oldest day's `trip_info` records if the table already holds more than 30 days of data, maintaining a strict 30-day rolling window. No-ops while the table is still filling up. |
| **23:50** | Trip data collection | Fetches all of today's departures for both directions from the TransitHub API, saves any new records to `trip_info`, and updates the `trip_info_metric` aggregates. |

---

## How personal information is stored

> **No personal data is ever sent to the server.**

The Profile drawer collects optional personal details to make it easier to fill in reimbursement claim forms:

- First name, last name
- Phone number, email address
- Home address, city, postal code
- Train ticket number
- Personal number (Swedish "personnummer")

### Encryption

When you click **Save**, the following happens entirely inside your browser:

1. A **256-bit AES-GCM key** is derived from `CRYPTO_SECRET` + `CRYPTO_SALT` using **PBKDF2-SHA-256** (100 000 iterations) via the browser's native `SubtleCrypto` API.
2. A random **96-bit IV** is generated.
3. Your profile JSON is **encrypted** with that key and IV.
4. The result (`hex(iv):hex(ciphertext)`) is written to **`localStorage`** under the key `userProfile`.

The plaintext is never sent to or stored on the server. Clearing your browser's local storage removes all saved data permanently.

### To delete your data

Open your browser's DevTools → **Application → Local Storage** → delete the `userProfile` key, or simply clear your site data for `localhost` / the hosted domain.

---

## Claim Button & Trip Tracking

When you view a trip card, a **Claim** button is shown for trips that are eligible for reimbursement (cancelled or ≥ 20 minutes late). Once you click **Claim** for a trip:

- The app sends your claim request using your saved profile details.
- That trip is marked as claimed in your browser's `localStorage`.
- The button is replaced with a static label: **Already Claimed**.
- You cannot claim the same trip again from the same browser.

This claimed-trip tracking is local to your browser and device. Clearing your browser's site data will reset the claimed status.

---

## Project structure

```
src/main/java/com/hs/railway_stats/
├── config/          # Station constants (Uppsala C, Stockholm C)
├── dto/             # API request/response records
├── external/        # TransitHub REST client
├── mapper/          # Maps API responses to internal DTOs
├── repository/      # JPA repositories + entities (TripInfo, TripInfoMetric, Translation)
├── service/
│   ├── TripInfoService / TripInfoServiceImpl          # Trip collection, retrieval, deletion, rolling-window pruning
│   ├── TripInfoMetricService / TripInfoMetricServiceImpl  # Metric upsert, query, departure times
│   ├── ClaimsService                                  # Claim URL generation
│   ├── TranslationService                             # Station name ↔ ID mapping
│   └── RateLimiterService                             # IP-based rate limiting
└── view/
    ├── TripInfoView.java     # Main view (route /)
    ├── MetricsView.java      # Metrics view (route /metrics)
    └── component/
        ├── TripStatsChart    # Vaadin wrapper for <trip-stats-chart> Chart.js web component
        ├── InputLayout       # Route selector (From/To labels + swap button) and date/filter controls
        ├── TripInfoCard      # Individual trip card + claim button
        ├── ProfileDrawer     # Slide-in profile + admin panel
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

## Database schema

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
| `average_minutes_late` | INTEGER | Rolling average minutes late across all recorded trips |
| `total_trips` | INTEGER | Total number of trips recorded for this slot |
| `total_reimbursable_trips` | INTEGER | Trips that were cancelled or ≥ 20 min late |
| `canceled_trip_dates` | TEXT[] | Dates on which this departure was cancelled |

### `translation`

| Column | Type | Description |
|---|---|---|
| `id` | INTEGER (PK) | Auto-generated |
| `station_id` | INTEGER | TransitHub numeric station ID |
| `station_name` | TEXT | Human-readable display name |
| `claims_station_id` | TEXT | Station identifier used in reimbursement claim URLs |

---

## License

This project is for personal use. No license is currently specified.

---

## Caching

Trip info queries are cached in-memory using [Caffeine](https://github.com/ben-manes/caffeine) for fast repeated access and reduced database load. The cache is configurable via `application.properties` or YAML:

```
tripinfo.cache.expiry.hours=24   # How long (in hours) each entry stays in cache (default: 24)
tripinfo.cache.max-size=100      # Maximum number of cached queries (default: 100)
```
Or in YAML:
```
tripinfo:
  cache:
    expiry:
      hours: 24
    max-size: 100
```
- **Cache keys** are based on origin, destination, and date.
- **Cache hits/misses** are logged: hits return cached data, misses query the DB and cache the result (unless empty).
- **Empty results** are not cached, so queries for missing data always check the DB.
- **Eviction:** If the cache exceeds the max size, least recently used entries are removed automatically.
