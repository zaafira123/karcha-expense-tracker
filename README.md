# Kharcha

Kharcha is a personal expense tracker for salaried professionals in India. Type what you spent in one sentence, such as "paid 450 for pizza with friends", and it records the amount, category, merchant, and date. It shows how much you can still spend this month, projects where the month is heading, and suggests moves between budgets when one category is running over.

**Live app:** No production URL is configured yet. This repository does not claim a live deployment.
**API health check:** `GET /api/health` (local: `http://localhost:8080/api/health`).

Password reset is not available in v1 because no email provider is configured; the Forgot password link is omitted.

## Features

- **Sentence entry:** type an expense in plain language. The app shows a preview you can correct before saving.
- **Automatic categories:** keyword rules first, with remembered corrections. AI categorisation is optional and disabled by default.
- **Monthly budgets:** a limit per category. Warnings appear at 80% and when over budget, written as text as well as colour.
- **Dashboard:** spent so far, budget remaining, projected month-end spend, and recent transactions.
- **Forecast:** a cumulative spending chart with the actual line, the projected line, a likely range, and the budget line.
- **Budget agent (approval only):** suggests moves from budgets with spare room to budgets that are running over. Approve, reject, or undo. Every decision is logged. Nothing changes without your approval.
- **Weekly and monthly summaries:** calculated from recorded spending, with category comparisons and three practical tips.
- **Account controls:** sign in and out, change the maximum agent move, and delete your account and all its data.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, TypeScript, Tailwind CSS, shadcn/ui, Recharts |
| Backend | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Flyway |
| Database | PostgreSQL (Neon in production, Docker locally) |
| AI | Google Gemini 3.5 Flash-Lite (primary), Groq (fallback), behind one `LlmClient` interface |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers, Vitest, React Testing Library |
| Deployment | Vercel (frontend), Render (backend, Docker), Neon (database) |

## Architecture

```
Browser
  │  Your verified production URL
  ▼
Vercel ── serves the React app
  │  /api/* is rewritten to the backend
  ▼
Render ── Spring Boot (Java 17), modular monolith
  ├─ auth/          sign in, sign up, JWT, refresh tokens
  ├─ transactions/  expenses: create, edit, delete, list
  ├─ budgets/       monthly limit per category
  ├─ parsing/       sentence → rules → AI fallback → validation
  ├─ forecast/      pure calculation of projections and range
  ├─ agent/         pure decision logic, proposals, audit log
  ├─ summary/       weekly and monthly numbers and template text
  └─ settings/      agent cap, account deletion
  │
  ├─ Neon (PostgreSQL)
  └─ Gemini API (Groq as fallback)
```

**Design decisions:**

- **The AI never decides amounts.** It categorises expense sentences when rules need help. Every number shown in the app is calculated by Java code.
- **The forecast and the agent are pure functions.** They take numbers in and return numbers out, with no database or AI calls inside. This makes them easy to test with fixed inputs.
- **The agent proposes, it does not act.** Each suggested move needs approval. Approved moves change budget amounts only, never bank balances. Each move can be undone for 7 days, and every step is written to an audit log.
- **Money is stored as whole paise.** No floating-point values are used for money.
- **One domain for the browser.** Vercel forwards `/api` calls to the backend, so login cookies work without cross-site settings.

## How the forecast works

1. Calculate the average daily spend over the last 30 full days. Days with no spending count as zero.
2. Projected month-end spend = spent so far + average daily spend × days left.
3. The likely range uses the variation in daily spending, widened over the remaining days.
4. The forecast needs at least 7 days with spending in the last 30. Otherwise the page asks for more data.

**Known limitation:** the forecast does not account for weekday patterns or one-off large purchases. Adding these is planned for a later version.

## How the budget agent decides

- **Receiver:** a category projected to finish above its budget.
- **Donor:** a category that is not protected and is projected to finish below 80% of its budget. Its spare amount is the part it can give up while still staying safely under 80%.
- **Amount:** the smallest of the receiver's shortfall, the donor's spare amount, and your maximum move (default ₹2,000, between ₹500 and ₹10,000).
- **Limits:** at most two suggestions per daily run. Suggestions expire after 7 days. No duplicate suggestions for the same receiver.
- **Protected categories:** Bills and utilities, Health, and Savings never give money.
- **Explanations:** a plain template is generated from the numbers. The current backend does not yet call AI to rewrite it.

## Categories

| Category | Protected |
|---|---|
| Food and dining | |
| Groceries | |
| Travel | |
| Shopping | |
| Bills and utilities | Yes |
| Health | Yes |
| Entertainment | |
| Savings | Yes |

## Parser accuracy

The parser is tested against 50 labelled sentences in `backend/src/test/resources/parser-eval.csv`. Each row includes the expected amount, category, and date.

**Result:** 50 of 50 correct (100%) in the current labelled evaluation set.

Run it yourself:

```bash
cd backend
./mvnw test -Dtest=ParserEvaluationTest
```

The evaluation set did not identify a failing sentence in this run. This score describes the labelled set only and does not guarantee accuracy for every sentence.

## Run it locally

### Prerequisites
- Java 17
- Maven (or use the included `./mvnw` wrapper)
- Node.js 18 or newer
- Docker (for the local PostgreSQL database)
- Optional: a Google Gemini or Groq API key only if AI features are enabled

### 1. Clone the repository
```bash
# Clone this repository using its URL from your Git hosting provider, then:
cd kharcha
```

### 2. Configure and start the local services

In PowerShell, set a local database password and a unique JWT secret. The Compose file has no committed JWT fallback:

```powershell
$env:DATABASE_PASSWORD = "choose-a-local-password"
$env:JWT_SECRET = "choose-a-random-secret-with-at-least-32-characters"
docker compose up -d db backend
```

The backend is available at `http://localhost:8080`. Its Compose datasource uses `jdbc:postgresql://db:5432/kharcha`. When connecting directly to Neon, use the JDBC form `jdbc:postgresql://host/database?sslmode=require` for `DATABASE_URL`.

Alternatively, create a root `.env` file with `DATABASE_PASSWORD` and `JWT_SECRET` set before running Compose. The public health endpoint is `GET /api/health`; it returns `ok` without checking the database.

### 3. Start the frontend

```powershell
cd frontend
npm install
npm run dev
```

The refresh cookie is `HttpOnly`, `Secure`, and `SameSite=Strict`. Production browser requests stay on the Vercel origin and are forwarded by the rewrite, so the cookie remains same-origin.

### Direct backend development

When starting Spring Boot outside Docker, configure `DATABASE_URL=jdbc:postgresql://localhost:5432/kharcha`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGIN`, `GEMINI_API_KEY`, `GROQ_API_KEY`, and `AI_DAILY_LIMIT_PER_USER` from your local environment, then run:

```powershell
cd backend
./mvnw spring-boot:run
```

The API listens on `PORT` when set, otherwise `8080`.

The app does not currently include a seed script. Create an account and add your own local data through the app.

## Run the tests

```bash
# Backend (unit and integration tests; Docker must be running for Testcontainers)
cd backend
./mvnw test

# Frontend
cd frontend
npm run test
npm run build
```

The current test suite includes unit and Testcontainers-backed integration tests. The parser evaluation result is recorded below.

Key test areas:
- Forecast: constant spend, zero spend, insufficient data, 28, 30, and 31-day months, first day of month
- Agent: no shortfall, cap applied, donor too small, protected donor rejected, two-suggestion limit, 80% boundary, duplicate suggestion, apply and undo, expired suggestion
- Parser: the 50-sentence evaluation set, rules-only path, mocked AI, invalid AI output rejected
- Auth: register, login, wrong password, protected route without token, users cannot see each other's data
- Integration: register, add an expense, and see it on the dashboard

## Deployment

### Database (Neon)
1. Create a free project at neon.tech.
2. Copy the connection string into the backend's `DATABASE_URL` setting.

### Backend (Render)
1. Create a new Web Service from this repository, with root directory `backend` and the Docker runtime. The multi-stage Dockerfile builds with Java 17 and runs as a non-root user.
2. Set `DATABASE_URL` to Neon's TLS-enabled `jdbc:postgresql://...?...sslmode=require` URL, plus `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGIN`, and `JWT_COOKIE_SECURE=true`. Generate a unique `JWT_SECRET` with at least 32 random bytes. Set `AI_ENABLED=false` and `SPRINGDOC_ENABLED=false`; only add Gemini/Groq keys if you intentionally enable AI. Render supplies `PORT`; the app listens on it and defaults to `8080` locally. Enter secrets only in Render's environment settings, never in Git or a `VITE_*` variable.
3. Set the health check path to `/api/health`.
4. Confirm your Render service's `/api/health` endpoint returns `ok`.

### Frontend (Vercel)
1. Import the repository, with root directory `frontend`.
2. Keep `VITE_API_BASE_URL` empty (the API is reached through the rewrite).
3. Before deploying, replace `https://YOUR-BACKEND.onrender.com` in `frontend/vercel.json` with the exact HTTPS URL of your Render service. The checked-in rewrite is a placeholder and is not production-ready.
4. Set `CORS_ALLOWED_ORIGIN` on Render to the exact production Vercel origin (scheme and host, no path); add a custom domain only if it is actually configured.
5. Confirm the rewrite and refresh each route after deployment. Confirm `GET /api/health` returns `ok` through the Vercel domain and verify sign-in, refresh, and sign-out over HTTPS.

The refresh-session migration invalidates refresh cookies issued before this feature. Existing users will need to sign in again after deployment.

## Security

- Passwords are hashed with BCrypt.
- Access tokens are kept in memory in the browser. Refresh tokens are stored in `HttpOnly` cookies, rotated on use, and tracked server-side only as SHA-256 hashes. Logout revokes the refresh session.
- Swagger/OpenAPI publishing is disabled by default; API paths are still discoverable by clients and are protected by authentication and ownership checks. Enable `SPRINGDOC_ENABLED=true` only for a trusted environment.
- The refresh-session migration invalidates refresh cookies issued before deployment; users will need to sign in again once after this update.
- Every request is validated on the server.
- Users can only read and change their own data. This is covered by tests.
- Failed login attempts are limited to five per IP address and email in a 15-minute window per running backend instance; successful sign-in clears that email's failure window.
- AI usage is limited per user per day.
- Secrets are read from environment variables. They are never committed to Git.

**Reporting a problem:** use the issue tracker for the repository where you obtained this source.

## Privacy

Kharcha stores your email address, a hashed password, and the expenses, budgets, and suggestions you create. For expense parsing, the configured AI service may receive the sentence you typed; the application calculates summaries itself. Your email and account details are not sent to AI. You can delete your account from Settings, which removes its expenses, budgets, proposals, and logs.

Full details: [Privacy Policy](/privacy) and [Terms of Service](/terms).

## Limitations

- No bank or wallet sync. Expenses are entered by you, by sentence or form.
- SMS import needs the user to paste or share the message. Automatic reading of messages is not possible in a web app. On Android, SMS permission is heavily restricted by Google Play; SMS-based money management may require policy review and approval.
- The forecast ignores weekday patterns and one-off purchases.
- The agent only suggests moves between budgets and never auto-applies them.
- Single currency (INR).
- Single user per account. No shared budgets.
- Free hosting can cause slow first loads.

## Roadmap

### Future developments: AI features (planned)

- AI parsing fallback for unknown merchants
- AI-written weekly or monthly summary wording
- AI-written budget-agent "Why?" explanations

AI is not required and is not included in the current version's required behavior. It is disabled by default with `AI_ENABLED=false`. The rules-based parser, Java summaries, and template explanations continue to work without API keys.

- Automatic apply for agent moves with per-user caps
- Weekday-aware and seasonal forecasting once there is enough history
- CSV import for bank statements
- Automatic import through an Account Aggregator adapter, and a native Android app with notification-based capture where the platform allows it.
- Email digests
- Shared budgets for couples and roommates

## Project structure

```
kharcha/
├── backend/            Spring Boot API (Java 17)
│   └── src/main/java/com/kharcha/
│       ├── auth/
│       ├── transactions/
│       ├── budgets/
│       ├── parsing/
│       ├── forecast/
│       ├── agent/
│       ├── summary/
│       └── settings/
├── frontend/           React app (Vite, TypeScript)
│   ├── src/
│   └── vercel.json
├── docs/
│   ├── design/         DESIGN.md and Stitch screen exports
│   └── screenshots/
├── docker-compose.yml
├── PRODUCT.md
├── AGENTS.md
└── README.md
```

## Acknowledgements

Layout inspiration from [Monarch](https://www.monarch.com/). Design created with Stitch by Google. Built with [Antigravity](https://antigravity.google/) as the coding agent.

## Licence

No licence has been selected for this repository. Do not assume permission to reuse or redistribute the source.
