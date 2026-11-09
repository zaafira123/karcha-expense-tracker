# Kharcha: Product Requirements (v1)

## What it is
Kharcha is a personal expense tracker for salaried professionals in India. You type what you spent in one sentence and it is recorded. It shows how much you can still spend this month, projects where the month is heading, and suggests moves between budgets when one category is running over.

It is a real, public website with user accounts and a live database. It is not a prototype.

## Problem
People stop tracking expenses because logging is slow and charts need interpreting. Month-end surprises happen because nobody checks the running total until it is too late.

## Target user
Salaried professionals who pay mostly by UPI and cards, and who want a quick view of their month without opening an app every day.

## Core promise
1. Logging an expense takes under 5 seconds.
2. You always see how much you can still spend this month, and where the month is heading.
3. When one budget is running over, the app suggests a fix. It never changes your money without your approval.

## Scope for v1 (in)

1. **Accounts:** Create account, sign in, sign out, delete account. Email and password. Password reset is in scope only if the email provider is set up; otherwise the "Forgot password" link is removed and the README says so.
2. **Add expense by sentence:** Type "paid 450 for pizza with friends". The app extracts amount, category, merchant, and date, shows a preview card, and saves only after the user confirms or edits.
3. **Manual add form:** Same fields, for when a sentence is not convenient. Also the fallback when the AI parse fails.
4. **Transactions:** List by month, filter by category, edit, delete (with confirmation).
5. **Categories:** Eight fixed categories:
   - Food and dining
   - Groceries
   - Travel
   - Shopping
   - Bills and utilities (protected)
   - Health (protected)
   - Entertainment
   - Savings (protected)
6. **Auto-categorisation:** Rules first (merchant keywords), then Gemini for unknown merchants. The user's corrections are remembered and reused.
7. **Monthly budgets:** A limit per category per month. Bars turn amber at 80 percent and red when over. Warnings are written as text, not colour alone.
8. **Dashboard:** Spent this month, budget remaining, projected month-end spend, days left, category bars, recent transactions, a weekly summary card, and the suggested move card when one is pending.
9. **Forecast:** A cumulative spend chart for the month. Solid line for actual spend, dashed line for projection, shaded band for the likely range, and a horizontal budget line.
10. **Budget agent (approval only):** Proposes moves between budgets. Approve, reject, and undo are available. Every decision is logged. Auto-apply is not in v1.
11. **Weekly and monthly summary:** A short plain-English summary with three tips. Calculated numbers come from Java; the AI only writes the wording.
12. **Settings:** Change the maximum agent move, sign out, delete account.
13. **Parser accuracy test:** A labelled set of 50 sample sentences with a test that reports accuracy. The result goes in the README.

## Scope for v1 (out)
- Bank, wallet, or crypto sync
- CSV or PDF statement import
- Automatic (unapproved) agent moves
- Cash-flow runway and bill calendar
- Custom categories and sub-categories
- Multiple accounts (cash, bank, card, wallet)
- Email or WhatsApp digests
- Multi-currency (INR only)
- Business mode, GST export, receipt scanning, voice entry
- Shared budgets and couples mode
- Seasonal forecasting

## Pages and routes

| URL | Visible to | Purpose |
|---|---|---|
| `/` | Everyone | Landing page: product explanation, features, FAQ |
| `/signin` | Everyone | Sign in and create account (tabs) |
| `/forgot-password` | Everyone | Reset request (only if email is set up) |
| `/dashboard` | Signed in | Home |
| `/add` | Signed in | Add expense (sentence or form) |
| `/transactions` | Signed in | List, filter, edit, delete |
| `/budgets` | Signed in | Monthly limits |
| `/forecast` | Signed in | Chart and projection |
| `/agent` | Signed in | Suggested moves and activity log |
| `/settings` | Signed in | Account, agent cap, sign out, delete account |
| `/privacy`, `/terms` | Everyone | Real legal pages |
| `*` | Everyone | Not found |

Signed-out visitors who open a protected route are sent to `/signin`, then returned to the page they asked for.

On mobile, the bottom navigation has five items: Home, Add, Transactions, Budgets, More. "More" opens Forecast, Agent activity, and Settings.

## Business rules

**Money**
- Stored as whole paise (integer). Never decimal or floating-point.
- Displayed as ₹ with Indian digit grouping (₹1,20,000).
- Currency is INR only.

**Dates and months**
- All "today" and "this month" logic uses Asia/Kolkata.
- Dates display as "19 Oct 2026".

**Forecast**
- Uses the average daily spend over the last 30 full days (zero-spend days count as 0).
- Projection = spent so far + average daily spend × days left.
- Range = projection ± (standard deviation of daily spend × √days left), never below spent so far.
- Needs at least 7 days, out of the last 30, with any spending. Otherwise the page says "Add a few more days of expenses for a forecast."
- Known limitation, stated in the README: it ignores weekday patterns and one-off large purchases.

**Budget agent**
- Runs every day at 09:00 IST, and after a transaction pushes a category past 80 percent of its budget.
- Receiver: a category whose projected month-end spend is above its budget.
- Donor: a non-protected category whose projected spend is below 80 percent of its budget. Spare amount = budget − 1.25 × projected spend.
- Each move is the smallest of: the receiver's shortfall, the donor's spare, and the user's maximum move (default ₹2,000, allowed ₹500 to ₹10,000).
- At most 2 proposals per daily run. Pending proposals expire after 7 days. No duplicate pending proposal for the same receiver.
- Protected categories (Bills and utilities, Health, Savings) never give money.
- Approving changes budget numbers only. Total budget stays the same. Real bank balances are never touched.
- Undo is available for 7 days after applying. If budgets were edited since, undo still works and the screen shows a note.
- Every proposal, decision, and undo is written to the audit log.
- Decisions are made by plain code. The AI only rewrites the "Why?" explanation, and the template explanation is shown if the AI is unavailable.

**AI usage**
- The AI never writes to the database and never decides amounts.
- Each user has a daily AI call limit. When the limit is reached, the app uses the rules-based path and shows a clear message.
- If the AI service is unavailable, the app still works: rules-based categorisation, manual entry, template summaries, and template explanations.

**Privacy**
- The AI receives only the text the user typed for parsing, and computed totals for summaries. Never names, emails, or account numbers.
- Deleting an account removes all its expenses, budgets, proposals, and logs.

## Success criteria for the week
- A new visitor can create an account and add 5 expenses in under 2 minutes, with no manual setup.
- Sign-in survives a page refresh. Sign-out clears the session.
- Budget changes update the dashboard, forecast, and agent suggestions.
- A suggested move can be approved and undone.
- Deleting an account removes its data.
- Every screen has loading, empty, error, and success states.
- Works on a 390px phone and a 1440px laptop.
- Reloading any URL works, with no 404.
- Backend tests pass for parsing, forecast, agent, and auth.
- Live on Vercel (frontend) and Render (backend) with a Neon database.
- README includes the parser accuracy result, the forecast limitation, the agent rules, and a live link.
- No placeholder text anywhere. The pricing FAQ is either answered or removed.

## Deployment
- Frontend: Vercel, from the `frontend/` folder.
- Backend: Render, Docker, from the `backend/` folder.
- Database: Neon PostgreSQL.
- AI: Google Gemini API (Flash-Lite as primary), Groq as fallback.
- Vercel rewrites `/api/*` to the Render backend, so the browser talks to one domain and login cookies work.
- Free hosting may sleep after inactivity. The README tells reviewers the first load can take 30 to 60 seconds.