# Kharcha

Kharcha is a personal expense tracker for salaried professionals in India. Type what you spent in one sentence, such as "paid 450 for pizza with friends", and it records the amount, category, merchant, and date. It shows how much you can still spend this month, projects where the month is heading, and suggests moves between budgets when one category is running over.

**Live app:** [https://your-app.vercel.app](https://your-app.vercel.app)
**API health check:** [https://your-api.onrender.com/api/health](https://your-api.onrender.com/api/health)

> The app runs on free hosting. If it has not been used for a while, the first load can take 30 to 60 seconds while the server wakes up.

## Screenshots

| Dashboard | Add expense | Forecast | Agent suggestion |
|---|---|---|---|
| ![Dashboard](docs/screenshots/dashboard.png) | ![Add expense](docs/screenshots/add-expense.png) | ![Forecast](docs/screenshots/forecast.png) | ![Agent](docs/screenshots/agent.png) |

## Features

- **Sentence entry:** type an expense in plain language. The app shows a preview you can correct before saving.
- **Automatic categories:** keyword rules first, then the Gemini AI for unknown merchants. Your corrections are remembered.
- **Monthly budgets:** a limit per category. Warnings appear at 80% and when over budget, written as text as well as colour.
- **Dashboard:** spent so far, budget remaining, projected month-end spend, and recent transactions.
- **Forecast:** a cumulative spending chart with the actual line, the projected line, a likely range, and the budget line.
- **Budget agent (approval only):** suggests moves from budgets with spare room to budgets that are running over. Approve, reject, or undo. Every decision is logged. Nothing changes without your approval.
- **Weekly and monthly summaries:** a short plain-English summary with three tips.
- **Account controls:** sign in and out, change the maximum agent move, and delete your account and all its data.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, TypeScript, Tailwind CSS, shadcn/ui, Recharts |
| Backend | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Flyway |
| Database | PostgreSQL (Neon in production, Docker locally) |
| AI | Google Gemini API (primary), Groq (fallback), behind one `LlmClient` interface |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers, Vitest, React Testing Library |
| Deployment | Vercel (frontend), Render (backend, Docker), Neon (database) |

## Architecture

```
Browser
  │  https://your-app.vercel.app
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
  ├─ summary/       weekly and monthly numbers and text
  └─ settings/      agent cap, account deletion
  │
  ├─ Neon (PostgreSQL)
  └─ Gemini API (Groq as fallback)
```

**Design decisions:**

- **The AI never decides amounts.** It reads sentences and writes explanations. Every number shown in the app is calculated by Java code.
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
- **Explanations:** a plain template is always generated from the numbers. The AI can reword it, and the template is shown if the AI is unavailable.

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

**Result:** [X] of 50 correct ([Y]%)

Run it yourself:

```bash
cd backend
./mvnw test -Dtest=ParserEvaluationTest
```

Known weak spots: [list the sentence types that failed, for example "sentences without an amount" or "merchants not in the keyword list"].

## Run it locally

### Prerequisites
- Java 17
- Maven (or use the included `./mvnw` wrapper)
- Node.js 18 or newer
- Docker (for the local PostgreSQL database)
- A Google Gemini API key (free tier, from Google AI Studio)

### 1. Clone the repository
```bash
git clone https://github.com/[your-username]/kharcha.git
cd kharcha
```

### 2. Start the database
```bash
docker compose up -d
```
This starts PostgreSQL on port 5432 with the database `kharcha`.

### 3. Configure the backend
```bash
cd backend
cp .env.example .env
```
Edit `.env` and set:

| Variable | Example | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/kharcha` | Database connection |
| `DATABASE_USERNAME` | `kharcha` | Database user |
| `DATABASE_PASSWORD` | `kharcha` | Database password |
| `JWT_SECRET` | a long random string (32+ characters) | Signs login tokens |
| `GEMINI_API_KEY` | your key | AI parsing and summaries |
| `GROQ_API_KEY` | your key (optional) | AI fallback |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | Allows the local frontend |
| `AI_DAILY_LIMIT_PER_USER` | `50` | Daily AI calls per user |

Then start the backend:
```bash
./mvnw spring-boot:run
```
The API runs on `http://localhost:8080`. Swagger documentation is at `http://localhost:8080/swagger-ui.html`.

### 4. Configure and start the frontend
```bash
cd ../frontend
cp .env.example .env.local
npm install
npm run dev
```
Set `VITE_API_BASE_URL=http://localhost:8080` in `.env.local` for local development. Open `http://localhost:5173`.

### 5. Load demo data (optional)
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```
This creates a demo account with 60 days of expenses and one pending agent suggestion.
Demo login: `demo@kharcha.app` / `[demo password from seed script]`

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

Current results: [N] backend tests passing, [M] frontend tests passing.

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
1. Create a new Web Service from this repository, with root directory `backend` and the Docker runtime.
2. Add the environment variables from the table above.
3. Set the health check path to `/api/health`.
4. Confirm `https://[your-api].onrender.com/api/health` returns `ok`.

### Frontend (Vercel)
1. Import the repository, with root directory `frontend`.
2. Set `VITE_API_BASE_URL` to empty (the API is reached through the rewrite).
3. `frontend/vercel.json` forwards `/api/*` to the Render backend and sends all other paths to `index.html`, so refreshing any page works.
4. Replace `YOUR-BACKEND` in `vercel.json` with your Render service name.

## Security

- Passwords are hashed with BCrypt.
- Access tokens are kept in memory in the browser. Refresh tokens are stored in `HttpOnly` cookies.
- Every request is validated on the server.
- Users can only read and change their own data. This is covered by tests.
- Login attempts are rate-limited.
- AI usage is limited per user per day.
- Secrets are read from environment variables. They are never committed to Git.

**Reporting a problem:** open a GitHub issue, or email [your-email].

## Privacy

Kharcha stores your email address, a hashed password, and the expenses, budgets, and suggestions you create. When parsing or summarising, the AI receives only the text you typed or computed totals, never your email or account details. You can delete your account from the Settings page, which removes all your data.

Full details: [Privacy Policy](/privacy) and [Terms of Service](/terms).

## Limitations

- No bank or wallet sync. Expenses are entered by you, by sentence or form.
- The forecast ignores weekday patterns and one-off purchases.
- The agent only suggests moves between budgets and never auto-applies them.
- Single currency (INR).
- Single user per account. No shared budgets.
- Free hosting can cause slow first loads.

## Roadmap

- Automatic apply for agent moves with per-user caps
- Weekday-aware and seasonal forecasting once there is enough history
- CSV import for bank statements
- Bank sync through an Account Aggregator adapter
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

[Choose a licence, for example MIT.]