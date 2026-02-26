# 🚆 Movingo Tracker (railway-stats)

A self-hosted web application for tracking train punctuality on the **Uppsala C ↔ Stockholm C** corridor. It automatically collects trip data every night, stores it locally, and lets you browse historical delay and cancellation stats — including which journeys qualify for a reimbursement claim.

---

## What it does

| Feature | Description |
|---|---|
| **Automatic data collection** | A scheduled job runs every night at **23:50 (Europe/Stockholm)** and fetches all departures for both directions (Uppsala → Stockholm and Stockholm → Uppsala) from the [TransitHub API](https://v2.api.transithub.se). |
| **Trip grid** | The main view shows a filterable grid of trips for any past date with columns: Departure, Arrival, Minutes Late, and Cancelled. |
| **Claimable filter** | A "Claimable" checkbox filters the grid to only show trips that were **cancelled** or **≥ 20 minutes late** — the Swedish threshold for a reimbursement claim. |
| **Swap button** | Quickly swap origin and destination to view the return leg. |
| **Profile drawer** | Optionally save your personal details (name, address, ticket number, etc.) in the browser for convenience when filing claims. All data is encrypted client-side. |
| **Rate limiter** | IP-based rate limiter (20 requests / 5 minutes, 15-minute block) protects the API endpoint from abuse. |

---

## Tech stack

- **Java 21** + **Spring Boot 4.x**
- **Vaadin 25** (server-side UI framework)
- **Spring Data JPA** + **H2** (dev) / **PostgreSQL** (prod)
- **Lombok**
- **Jackson** (JSON serialization)

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
  java -jar target/railway-stats-*.jar
```

---

## How personal information is stored

> **No personal data is ever sent to the server.**

The Profile drawer collects optional personal details to make it easier to fill in reimbursement claim forms:

- First name, last name
- Phone number, email address
- Home address, city, postal code
- Train ticket number

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

## Project structure

```
src/main/java/com/hs/railway_stats/
├── config/          # Station constants (Uppsala C, Stockholm C)
├── dto/             # API request/response records
├── external/        # TransitHub REST client
├── mapper/          # Maps API responses to internal DTOs
├── repository/      # JPA repositories + entities (TripInfo, Translation)
├── service/         # Business logic, scheduler, rate limiter
└── view/            # Vaadin UI (main view + components)
    ├── component/   # ProfileDrawer, TripInfoGrid, InputLayout, …
    └── util/        # BrowserStorageUtils (client-side crypto)
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

---

## License

This project is for personal use. No license is currently specified.

