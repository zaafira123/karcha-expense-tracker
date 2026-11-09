# Kharcha: Agent Rules

## Read first, every session
1. `PRODUCT.md`: scope, routes, business rules.
2. `docs/design/DESIGN.md`: all UI decisions.
3. `docs/design/screens/`: approved Stitch layouts.
4. This file.

Do not add features outside `PRODUCT.md`. If something seems missing or conflicting, stop and ask.

## Project shape
- Monorepo: `backend/` (Spring Boot), `frontend/` (React), `docs/`, root docs.
- This is a real public website. Every feature must work end to end against the database.

## Backend stack
- Java **17** (LTS). Do not use Java 21 features such as virtual threads or records with newer syntax that Java 17 does not support. Check the compiler target before using a newer API.
- Spring Boot 3.x (compatible with Java 17), Maven.
- Spring Web, Spring Security (JWT), Spring Data JPA, Flyway, PostgreSQL driver, Bean Validation, springdoc-openapi.
- Tests: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL).

## Frontend stack
- React with Vite and TypeScript.
- Tailwind CSS, with DESIGN.md tokens defined in the Tailwind config first.
- shadcn/ui components, restyled to DESIGN.md. Do not keep the default shadcn theme colours.
- Recharts for the forecast chart.
- Testing: Vitest and React Testing Library for components that have logic.

## AI stack
- Google Gemini API (JSON schema output, Flash-Lite as primary model).
- Groq as fallback.
- Both behind an `LlmClient` interface. Never call an AI SDK directly from a controller or from a business service.
- All AI calls have a timeout and one retry.

## Money and dates
- Store money as `BIGINT` paise. Never `double`, `float`, or `BigDecimal` in calculations that must round exactly. Use integer math.
- Display in ₹ with Indian digit grouping.
- Every "today", "month", and "week" calculation uses `Asia/Kolkata`.

## Architecture rules
- Modular monolith: packages `auth`, `transactions`, `budgets`, `parsing`, `forecast`, `agent`, `summary`, `settings`.
- Controllers are thin: validate input, call a service, return a DTO.
- The forecast and the agent decision logic are pure Java classes with no database, no HTTP, and no LLM calls. They take numbers in and return numbers out.
- The LLM never writes to the database and never decides any amount.
- Every change to budgets (approve, undo, manual edit) writes to `agent_audit` or the matching audit path.
- Use one enum for categories with a `protected` flag. Protected: `BILLS_UTILITIES`, `HEALTH`, `SAVINGS`.

## Security rules
- Passwords hashed with BCrypt. Never log passwords, tokens, or API keys.
- Access tokens are short-lived and kept in memory on the frontend.
- Refresh tokens are stored in `HttpOnly`, `Secure`, `SameSite` cookies. Never in `localStorage`.
- Every request body is validated on the server. Errors return one consistent JSON shape: `{ "error": "code", "message": "text for the user", "fields": {...} }`.
- Users can only read or change their own rows. Test this for every resource endpoint.
- Secrets come from environment variables only. Keep `.env.example` with blank values. Never commit `.env`.
- Rate-limit login attempts per IP and per email.
- Daily AI call limit per user. When reached, return the rules-based result and a clear message.

## Required environment variables
Backend: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `GEMINI_API_KEY`, `GROQ_API_KEY`, `CORS_ALLOWED_ORIGIN`, `AI_DAILY_LIMIT_PER_USER`.
Frontend: `VITE_API_BASE_URL` (empty in production, so calls go to `/api` through the Vercel rewrite).

## API conventions
- Every backend route starts with `/api`.
- Paths and fields are in the API contract (OpenAPI). Keep the contract and the code in sync.
- Monetary fields are named with the `Paise` suffix in JSON, for example `amountPaise`. The frontend formats them for display.

## Routing and deployment
- Frontend on Vercel. `frontend/vercel.json` rewrites `/api/:path*` to the Render backend and sends all other paths to `/index.html`, so refreshing any route works.
- Backend on Render, Docker, root directory `backend/`.
- Database on Neon. Connection string in the Render environment variables.
- Confirm the Vercel rewrite and the Render health check (`GET /api/health`) after every deploy.

## Working product rules
- No hardcoded demo numbers in production code. Demo data comes only from a seed script run against a local or demo database.
- Every screen implements loading, empty, error, and success states.
- Every route in `PRODUCT.md` exists. Protected routes redirect to `/signin` when signed out.
- No placeholder text in the UI. If an answer is not ready, remove the item.
- Forgot password is built only if an email provider is connected. Otherwise remove the link.
- Delete account requires a confirmation step and removes all the user's data.

## Testing rules
- Write tests with each feature. Forecast and agent tests come first.
- Mock Gemini and Groq in every test. Never call a live AI API in the test suite.
- Required backend tests:
  - Forecast: constant spend, zero spend, insufficient data, 28/30/31-day months, first day of month.
  - Agent: all cases from the agent test table (no shortfall, one move, cap applied, donor spare smaller than shortfall, protected donor rejected, two-proposal limit, 80% boundary, pending duplicate, apply and undo, expired proposal).
  - Parser: the 50-sentence evaluation set, rules-only path, mocked AI, invalid AI output rejected.
  - Auth: register, login, wrong password, protected route without token returns 401, user cannot read another user's data.
  - Integration (Testcontainers): register, add expense, see it on the dashboard.
- Target: all unit tests under 30 seconds.
- Before saying a task is done: run the backend tests, run the frontend build, and confirm the feature works in the running app.

## Design rules (frontend)
- Build each screen to match its Stitch export in `docs/design/screens/`.
- Use only the colour tokens in DESIGN.md. No gradients, shadows on cards, dark mode, emoji, or icon-only buttons.
- One primary button per visible area.
- Money always shows ₹ and Indian grouping, with tabular numbers.
- Status always has text, not colour alone.
- If a Stitch screen and DESIGN.md disagree, stop and ask.

## Working style
- Small changes, one feature at a time.
- Show the plan before any large edit or new package.
- Explain any code I ask about in plain language.
- Do not add dependencies without saying why.
- Keep commits small, with clear messages.

## Definition of done (for the final release)
- Create account, sign in, add expense by sentence, see it on the dashboard without reloading.
- Budget changes update the dashboard, forecast, and agent suggestions.
- Approve and undo an agent move.
- Delete account removes all its data.
- Every screen has all four states.
- Works on a 390px phone.
- Reloading any URL works.
- No API keys in the browser or Git history.
- Privacy and Terms pages open.
- README includes the parser accuracy result, the forecast limitation, the agent rules, and the live link.