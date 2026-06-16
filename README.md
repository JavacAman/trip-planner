# TripPlanner AI

An AI-powered travel itinerary generator built as a full-stack assessment project. Users submit a destination, trip duration, budget, and interests; an LLM hosted on Groq generates a complete day-by-day itinerary, budget estimate, and hotel suggestions in seconds.

**Live demo**: Frontend → [dapper-shortbread-f0ad2e.netlify.app](https://dapper-shortbread-f0ad2e.netlify.app) · Backend API → [trip-planner-production-bc80.up.railway.app](https://trip-planner-production-bc80.up.railway.app)

---

## Tech Stack & Justification

| Layer | Choice | Rationale |
|-------|--------|-----------|
| Backend | Java 17 + Spring Boot 3.2 | Strong typing, mature ecosystem, Spring Security handles auth seamlessly |
| Database | MySQL 8 + Spring Data JPA | ACID-compliant relational DB; trip data has clear relational structure |
| Cache | Redis | Expensive AI calls are cached (6h TTL for itineraries) to cut latency and API costs |
| AI | Groq API (Llama 3.3 70B) | Free tier with generous limits, OpenAI-compatible API, very low inference latency |
| Frontend | Angular 17 (standalone) | Reactive forms, built-in DI, HTTP interceptors — production-ready without extra libraries |
| Styling | Tailwind CSS | Utility-first, fast iteration, zero unused CSS in prod build |
| DevOps | Docker + GitHub Actions | Reproducible builds; automated test → build → deploy pipeline |
| Deploy | Railway (backend) + Netlify (frontend) | Railway handles JVM containers well; Netlify CDN is ideal for Angular SPAs |

---

## Project Structure

```
trao/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/trao/tripplanner/
│   │   ├── config/             # SecurityConfig, RedisConfig, SwaggerConfig
│   │   ├── controller/         # AuthController, TripController
│   │   ├── dto/                # Request & response DTOs
│   │   ├── exception/          # GlobalExceptionHandler + custom exceptions
│   │   ├── model/              # User, Trip, DayItinerary, Activity
│   │   ├── repository/         # Spring Data JPA repositories
│   │   ├── security/           # JWT provider, filter, UserDetailsService
│   │   └── service/
│   │       ├── strategy/       # ItineraryGenerationStrategy (Strategy Pattern)
│   │       └── observer/       # TripEvent + TripEventListener (Observer Pattern)
│   └── src/test/               # JUnit 5 + Mockito tests
├── frontend/                   # Angular 17 SPA
│   └── src/app/
│       ├── core/               # Services, guards, interceptors, models
│       ├── features/           # auth, dashboard, trip-planner, itinerary
│       └── shared/             # Reusable components (navbar, spinner, budget-card)
├── .github/workflows/          # CI/CD pipeline
├── docker-compose.yml          # Full local stack
└── .env.example                # Environment variable template
```

---

## Local Setup

### Prerequisites
- Java 17+, Maven 3.9+
- Node 20+, npm
- Docker & Docker Compose
- A Groq API key from [console.groq.com/keys](https://console.groq.com/keys)

### 1. Clone & configure

```bash
git clone https://github.com/JavacAman/trip-planner.git
cd trip-planner
cp .env.example .env
# Edit .env and set GROK_API_KEY and a strong JWT_SECRET
```

### 2. Run with Docker Compose (recommended)

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

### 3. Run manually (development)

**Backend**
```bash
cd backend
# Ensure MySQL is running, then:
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--GROK_API_KEY=your_key --JWT_SECRET=your_secret"
```

**Frontend**
```bash
cd frontend
npm install
npm start           # serves on http://localhost:4200
```

### 4. Run tests

```bash
# Backend (uses H2 in-memory for tests)
cd backend && mvn test

# Frontend
cd frontend && npm test
```

---

## Architecture

### High-Level Flow

```
Angular SPA  →  JWT Interceptor  →  Spring Backend  →  MySQL
                                           ↓
                                     Redis Cache
                                           ↓
                                      Groq API
```

### Request Lifecycle

1. User fills trip form → Angular reactive form validates client-side
2. `TripService` (Angular) calls `POST /api/v1/trips` with JWT header (injected by `jwtInterceptor`)
3. `JwtAuthenticationFilter` validates token → populates `SecurityContext`
4. `TripController` delegates to `TripService` (Spring)
5. `TripService` calls `GrokAiService.generateContent()` with a prompt built by `DetailedItineraryStrategy`
6. `ItineraryParserService` maps the JSON response to JPA entities
7. All data persists atomically (`@Transactional`)
8. Spring `ApplicationEventPublisher` fires `TripEvent` (Observer pattern) for async logging
9. Response is cached in Redis; subsequent GET requests for the same trip are served from cache

---

## Authentication & Authorization

- **Registration**: Password hashed with BCrypt (cost 12) before storage
- **Login**: `AuthenticationManager` validates credentials → generates 24h JWT
- **Authorization**: Stateless JWT-based; `SecurityContext` populated per request by `JwtAuthenticationFilter`
- **Data isolation**: Every data query is scoped by `userId` — e.g. `findByIdAndUserId()` — so users cannot read or modify each other's trips
- **Route protection (Angular)**: `authGuard` (authenticated only) and `guestGuard` (unauthenticated only) protect all routes

---

## AI Agent Design

The AI layer uses Grok's chat completions endpoint via `WebClient` (non-blocking reactive HTTP).

**Strategy Pattern** (`ItineraryGenerationStrategy`):
- `DetailedItineraryStrategy` is the current concrete strategy
- Adding a new strategy (e.g. `BriefItineraryStrategy`, `ThemeBasedStrategy`) requires only a new class; no changes to `TripService`
- The strategy builds structured JSON prompts that instruct Grok to respond with a strict schema

**Prompt Engineering**:
- System prompt mandates JSON-only output
- User prompt injects destination, days, budget, and interests
- Response is stripped of any markdown fences (defensive parsing)

**Caching**:
- Itineraries are cached in Redis with a 6-hour TTL
- Hotel suggestions cached for 12 hours
- `@CacheEvict` on mutations keeps the cache consistent

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `ItineraryGenerationStrategy` + `DetailedItineraryStrategy` | Swap prompt-building algorithms without changing `TripService` |
| **Factory** | `ApiResponse.success()/.error()`, `AuthService.buildAuthResponse()` | Centralize DTO construction, eliminate duplication |
| **Builder** | Lombok `@Builder` on all entities and DTOs | Handle optional fields cleanly; immutable-friendly construction |
| **Singleton** | All Spring `@Service`, `@Component`, `@Repository` beans | Default Spring scope; one instance per application context |
| **Observer** | `TripEvent` + `ApplicationEventPublisher` + `TripEventListener` | Decouple side-effects (logging, future: notifications) from core logic |

---

## SOLID Principles

- **SRP**: Every class has one job — `GrokAiService` only talks to Grok; `ItineraryParserService` only parses JSON; `AuthService` only handles auth
- **OCP**: New AI strategies, new event handlers, and new response types extend the system without modifying existing code
- **LSP**: `ItineraryGenerationStrategy` implementations are interchangeable without altering callers
- **ISP**: Repository interfaces expose only the queries each consumer needs
- **DIP**: `TripService` depends on the `ItineraryGenerationStrategy` abstraction, not `DetailedItineraryStrategy` directly

---

## Creative / Custom Feature: Redis-Backed AI Response Caching

**Problem it solves**: Grok API calls take 5–15 seconds and cost money per token. Without caching, every page reload regenerates the entire itinerary, creating a terrible UX and burning through API quota.

**Solution**: A three-tier Redis cache:
- `itinerary_cache` — keyed by `tripId-dayNumber`, TTL 6h
- `hotel_cache` — keyed by destination+budget, TTL 12h
- `trip_cache` — keyed by user email, TTL 10min

Cache invalidation uses `@CacheEvict` annotations on write operations. This means:
- First generation: ~10s (Grok API call)
- Subsequent loads: <50ms (Redis hit)
- Day regeneration invalidates only that day's cache, not the whole trip

**Engineering judgement**: I chose Redis over Caffeine (in-process cache) because it survives pod restarts (important on Railway's ephemeral containers) and is shared across potential horizontal replicas.

---

## Creative / Custom Feature: Smart Re-planner

Built Smart Re-planner because weather disruptions and unexpected situations are the #1 cause of ruined trips. Instead of manually replanning, AI intelligently adapts the itinerary based on the specific issue reported.

**How it works**: Each day card has a "⚠️ Report Issue" button with four quick-pick options:
- 🌧️ Bad Weather — swaps outdoor activities for indoor alternatives
- 🤒 Not Feeling Well — replaces the day with a lighter, low-energy plan
- 💸 Over Budget — swaps activities for cheaper or free alternatives
- 🕐 Running Late — compresses and reorders the day into a shorter plan

Selecting an option sends an issue-specific instruction to the existing `POST /api/v1/trips/{tripId}/days/{dayNumber}/regenerate` endpoint, which reuses `DetailedItineraryStrategy.buildDayRegenerationPrompt()` to regenerate only that day — the rest of the itinerary is untouched.

---

## ACID Transactions

All write operations in `TripService` are annotated with `@Transactional`:
- **Atomicity**: If Grok returns a result but the DB save fails, the transaction rolls back — no partial trip state
- **Consistency**: Foreign key constraints and `@NotNull` validations enforce data integrity
- **Isolation**: Default `READ_COMMITTED` prevents dirty reads
- **Durability**: MySQL's InnoDB engine writes committed transactions to disk

---

## Design Decisions & Trade-offs

| Decision | Why | Trade-off accepted |
|---|---|---|
| Relational `DayItinerary`/`Activity` entities instead of a JSON blob column | Enables targeted queries (`findByTripIdAndDayNumber`), per-activity add/remove, and FK-enforced data integrity | More mapping/parsing code in `ItineraryParserService` than a single JSON column would need |
| Redis over in-process (Caffeine) cache | Survives container restarts on Railway's ephemeral filesystem and is shareable across horizontal replicas | Adds a network hop and an extra infra dependency for local dev (`docker-compose` runs Redis alongside MySQL) |
| Strategy pattern for prompt generation instead of inline prompt strings | New itinerary styles (e.g. budget-only, luxury-only) can be added without touching `TripService` | Slight indirection for a single-strategy app today |
| Smart Re-planner reuses the existing day-regeneration endpoint instead of a new one | The endpoint already accepts a free-text instruction and replaces exactly one day — no new controller/DTO/strategy surface needed | Issue-type metadata (e.g. "this day was replanned for bad weather") isn't persisted; only the resulting itinerary is |
| JWT in `Authorization` header (not httpOnly cookies) | Simplest to wire through Angular's `HttpInterceptor` and works identically across Netlify/Railway's different domains | Vulnerable to XSS-based token theft if the frontend ever introduces unsanitized HTML rendering — acceptable for an assessment scope, would need cookie-based auth + CSRF protection for a real production app |
| `findByIdAndUserId` everywhere instead of "load then check owner" | Returns 404 instead of 403 for another user's trip, so an attacker can't even confirm a trip ID exists | Slightly less specific error messages for legitimate ownership-mismatch cases |

---

## Known Limitations

1. **Grok latency**: Initial itinerary generation takes 5–15 seconds. A streaming endpoint (`/stream`) would improve perceived performance but requires SSE support on the Angular side.
2. **Hotel data**: Hotel suggestions are AI-generated (not real-time booking API data). Integrate with Booking.com or Amadeus API for live availability.
3. **Redis single-node**: The current setup uses a standalone Redis. For production HA, configure Redis Sentinel or Cluster.
4. **No email verification**: User registration doesn't require email confirmation. Add Spring Mail + a verification token flow for production.
5. **Image generation**: The UI would benefit from destination hero images. Could integrate Unsplash API keyed by destination.

---

## API Reference (Swagger)

Full interactive documentation available at `/swagger-ui.html` when running locally.

Key endpoints:

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/register` | No | Register new user |
| POST | `/api/v1/auth/login` | No | Login, receive JWT |
| POST | `/api/v1/trips` | JWT | Create trip + generate itinerary |
| GET | `/api/v1/trips` | JWT | List user's trips (paginated) |
| GET | `/api/v1/trips/{id}` | JWT | Get specific trip |
| POST | `/api/v1/trips/{id}/days/{day}/regenerate` | JWT | Regenerate a day with AI |
| POST | `/api/v1/trips/{id}/activities` | JWT | Add activity to a day |
| DELETE | `/api/v1/trips/{id}/activities/{actId}` | JWT | Remove activity |
| DELETE | `/api/v1/trips/{id}` | JWT | Delete trip |

---

## Deployment

### Backend → Railway

`backend/railway.json` pins the build/healthcheck config, so Railway needs no manual dashboard setup beyond env vars:

1. Create a Railway project
2. Add MySQL and Redis services
3. Set environment variables from `.env.example`
4. Connect your GitHub repo — Railway detects `Dockerfile` + `railway.json` and deploys automatically, health-checking `/actuator/health`

### Frontend → Netlify

`frontend/netlify.toml` pins the build command, publish directory, and SPA fallback redirect, so Netlify needs no manual dashboard setup:

1. Connect repo to Netlify
2. Netlify reads `netlify.toml` automatically (build command, publish dir, SPA redirect)
3. Set `NODE_ENV=production` in Netlify environment settings
4. Update `environment.prod.ts` with your Railway backend URL

### CI/CD Secrets Required

| Secret | Where | Value |
|--------|-------|-------|
| `RAILWAY_TOKEN` | GitHub Secrets | From Railway dashboard |
| `NETLIFY_AUTH_TOKEN` | GitHub Secrets | From Netlify user settings |
| `NETLIFY_SITE_ID` | GitHub Secrets | From Netlify site settings |
| `GROK_API_KEY` | Railway env vars | Your Groq API key |
| `JWT_SECRET` | Railway env vars | 64-char hex string |
