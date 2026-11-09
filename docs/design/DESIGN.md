# Kharcha: Design System

## Direction
Light, formal, and calm, in the style of established finance products such as Monarch and bank statement views. White surfaces, thin grey borders, dark text, and one restrained blue accent. Numbers are the hero. Everything else supports them.

Not allowed: dark colours or dark mode, gradients, glassmorphism, glows, shadows on cards, emoji, decorative illustrations, stock photos of people, animated backgrounds, icon-only buttons.

Visual references: the Stitch exports in `docs/design/screens/` are the approved layouts. Match them. Where Stitch and this file disagree, this file wins and the mismatch goes to the developer for a decision.

## Colour tokens

| Token | Hex | Use |
|---|---|---|
| `bg/canvas` | #FFFFFF | Page background, cards |
| `bg/subtle` | #F7F8FA | Sidebar, table header, section backgrounds |
| `bg/hover` | #F2F4F7 | Hover states, skeleton blocks |
| `bg/accent-soft` | #E9F0F7 | Active nav, selected chip, CTA section |
| `border/default` | #E4E7EC | Card borders, dividers |
| `border/strong` | #D0D5DD | Input borders, suggested move card |
| `text/primary` | #101828 | Headings, amounts, body |
| `text/secondary` | #475467 | Labels, descriptions |
| `text/muted` | #667085 | Captions, helper text, placeholders |
| `accent/default` | #1F4E79 | Primary buttons, active nav, progress fill, links |
| `accent/hover` | #173B5C | Primary button hover |
| `status/positive-text` | #067647 | Saved confirmations, text |
| `status/positive-bg` | #ECFDF3 | Positive background |
| `status/warning-text` | #B54708 | 80 to 100 percent of budget, "Projected over" |
| `status/warning-bg` | #FFFAEB | Warning background |
| `status/danger-text` | #B42318 | Over budget, errors, delete |
| `status/danger-bg` | #FEF3F2 | Danger background |

**Contrast:** body text must reach 4.5:1 on white. Verify with a contrast checker before handoff.

**Colour use:** `status/danger` is only for overspend and form errors. Never colour a whole card red. Pair every status colour with text.

## Typography
Font: **Inter** (fallback: system-ui, Segoe UI, Arial).
All amounts use tabular figures (`font-variant-numeric: tabular-nums`) and right alignment in tables.

| Style | Size / line height | Weight | Use |
|---|---|---|---|
| `display` | 32 / 40 | 600 | Main amounts (stat cards) |
| `h1` | 24 / 32 | 600 | Page titles |
| `h2` | 18 / 28 | 600 | Card titles |
| `body` | 15 / 24 | 400 | Default text |
| `body-strong` | 15 / 24 | 500 | Merchant names, emphasis |
| `small` | 13 / 20 | 400 | Helper text, meta, table secondary text |
| `caption` | 12 / 16 | 500 | Column headers, small labels |
| `landing-h1` | 48 / 56 | 600 | Landing headline only |
| `landing-h2` | 32 / 40 | 600 | Landing section headings only |

Minimum text size: 12px. Amounts always include ₹ and Indian grouping.

## Spacing and layout
- Base unit 4px. Scale: 4, 8, 12, 16, 24, 32, 48, 64, 96.
- Content maximum width: 1120px.
- Desktop outer margin 32px, column gutter 24px.
- Mobile side margin 16px.
- Card padding 24px (16px on mobile).
- Landing section spacing: 96px desktop, 64px mobile.
- Signed-in sidebar: 240px, `bg/subtle`, right border `border/default`.
- Mobile bottom bar: white, top border `border/default`, 5 items (Home, Add, Transactions, Budgets, More).

## Shape and elevation
- Radius: 8px for buttons, inputs, and chips. 12px for cards and modals. Nothing else.
- Cards: white, 1px `border/default`, no shadow.
- Dropdown menus only: `0 4px 12px rgba(16, 24, 40, 0.08)`.
- Focus ring: 2px `accent/default` outline with a 2px white offset. Always visible.

## Components

**Button, primary:** `accent/default` background, white text, body-strong, height 40px, padding 0 16px, radius 8px. Hover `accent/hover`.
**Button, secondary:** white background, 1px `border/strong`, `text/primary`. Hover `bg/hover`.
**Button, ghost:** no border, `accent/default` text. Hover `accent/soft`.
**Button, danger outline:** white, 1px `status/danger-text` border, danger text. Used only for "Delete account".
Rule: one primary button per visible area.

**Input:** height 40px, radius 8px, 1px `border/default`, label above in small `text/secondary`. Focus: `border/strong` plus focus ring. Error: 1px `status/danger-text` border and error message below in danger text.
**Large input (Add expense):** height 56px, 17px text, 1px `border/strong`.

**Stat card:** label small `text/secondary`, value `display` `text/primary`, note small `text/muted`.

**Category row:** name left (body), amount right (body-strong, tabular). 6px track `bg/subtle`, fill `accent/default`. Fill turns to warning colour at 80 percent, and a text status line appears under the bar ("96% used"). Over budget: text "Over by ₹1,800" in danger text.

**Transaction row (desktop):** 56px tall. Date small `text/muted` (80px), merchant body-strong, category small `text/secondary` (text only), amount right-aligned. Actions "Edit" and "Delete" as ghost buttons, shown on hover.
**Transaction row (mobile):** merchant and amount on line one; category and date on line two; "Edit" and "Delete" always visible as small accent text buttons.

**Chip (filter):** height 32px, radius 16px. Default: white, 1px `border/default`, `text/secondary`. Selected: `accent/soft` background, `accent/default` text and border.

**Preview card (parsed expense):** white card, 1px `border/strong`, 24px padding. Four labelled fields (Amount, Category, Merchant, Date). Helper text: "Parsed from your sentence. Check the amount before saving." Buttons: Cancel (ghost), Save expense (primary).

**Suggested move card:** white, 1px `border/strong`. Caption "SUGGESTED MOVE" in `text/muted` (the only uppercase label). Body-strong sentence. Small explanation. Buttons: Approve (primary), Reject (secondary), Why? (ghost). Line under buttons: "Nothing changes until you approve."

**Forecast chart:**
- Actual line: 2px solid `accent/default`, dot at today labelled with the actual amount.
- Projected line: 2px dashed `text/secondary`. Projection never uses the accent colour.
- Range band: `bg/accent-soft`.
- Budget line: 1px solid `status/warning-text`, labelled "Budget ₹42,000".
- Projected endpoint over budget: text label "₹X over", in danger text.
- Axes: small `text/muted`. Gridlines `border/default`.
- Legend under the title: "Actual", "Projected", "Likely range".

**Toast:** white, 1px `border/default`, 3px left stripe (`status/positive-text` for saved, `status/danger-text` for errors). Bottom centre on desktop, top on mobile. Auto-dismiss after 4 seconds.

**Empty state:** one line of body text explaining what appears here, one primary button. No illustrations.

**Error state:** white card, 1px `status/danger-text` border, title "Something went wrong", one sentence in `text/secondary`, secondary "Try again" button.

**Loading state:** skeleton blocks in `bg/hover` in the shape of the final cards and rows. No full-page spinners.

**Protected label:** small `text/muted` text "Protected" beside the category name. No lock icon.

## Navigation
- Public pages: top bar, white, 72px, 1px bottom border. Logo left. Links centre (Features, How it works, Who it's for, FAQ). Sign in (secondary) and Get started (primary) right.
- Signed in, desktop: sidebar with Dashboard, Add expense, Transactions, Budgets, Forecast, Agent activity. Settings and the user's email at the bottom.
- Signed in, mobile: top bar with page title, bottom bar with five items. Active item `accent/default`, inactive `text/muted`.

## Screens (build in this order; see `docs/design/screens/`)
1. Landing page
2. Sign in and create account (tabs)
3. Forgot password (only if email reset is built)
4. Dashboard
5. Add expense (sentence, preview card, manual form, error state)
6. Transactions (desktop table, mobile list, empty state)
7. Budgets (eight rows, Protected labels, inline edit)
8. Forecast (chart, pace, biggest driver, insufficient data state)
9. Agent activity (table, empty state, detail panel)
10. Settings (account, agent cap, sign out, delete account)
11. Not found
12. Session expired (sign-in card with message)
13.status loading empty error

## Sample data for design work
The same numbers appear on every screen so the design reads as one product:
- Month: October 2026. Today: 19 Oct 2026. 12 days left.
- Budget ₹42,000. Spent ₹28,450 (68%). Remaining ₹13,550.
- Projected month end ₹38,400, ₹3,600 under budget. Range ₹35,900 to ₹41,200.
- Average daily spend ₹829.
- Food and dining: budget ₹6,000, spent ₹4,200, projected ₹7,800 (₹1,800 over).
- Groceries ₹8,000 (spent ₹3,400). Travel ₹3,000 (₹1,250). Shopping ₹5,000 (₹4,800, 96%). Bills and utilities ₹9,000 (₹6,300, protected). Health ₹2,000 (₹380, protected). Entertainment ₹1,500 (₹1,220, 81%). Savings ₹7,500 (₹6,900, protected).
- Suggested move: ₹1,800 from Groceries to Food and dining. Monthly maximum ₹2,000.

## Copy rules
- Short and specific: "₹2,400 left for food this month", not "You're doing great!"
- No marketing language inside the app.
- No placeholder text in the product. The only allowed placeholder is the pricing FAQ answer, which must be filled before launch.
- Error messages say what to do next.

## Motion
Only two animations:
1. Category bars fill from 0 over 300ms, ease-out, on first load.
2. The preview card fades in and moves up 8px over 200ms.
Both are disabled under `prefers-reduced-motion`.

## Accessibility
- Every input has a visible label.
- Status is never shown by colour alone.
- Minimum tap target 40 × 40px.
- Full keyboard navigation, including the preview card and the agent detail panel.
- Focus is always visible.
- Test on Chrome desktop and an Android phone.