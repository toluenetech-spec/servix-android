# Servix Android — Release 1.1 "Chats" · Stage 0 — Information-architecture audit

**Status:** proposal for owner approval. **No feature code is written in this stage.**
**App version audited:** 1.0.1 / 101 (this branch also carries the "Bug fix pass 1.0.1 — profile photos",
which is **not yet merged to `main`** — PR #4 is open, so it folds into Stage 0 exactly as instructed).
**Plan:** `toluenetech-spec/servix` → `docs/android/PROMPT_CHAT_RELEASE_1.1.md` (Stage 0).

---

## 1. What the audit found

The owner's words were *"not everything on the web is in the app, and what is in the app is not well arranged —
some things are shown that aren't needed, and things that are needed are missing."* Three findings explain all
of it:

| # | Finding | Evidence |
|---|---|---|
| F1 | **The app never reads `GET /features`.** Eleven server flags exist (`requests, compare, achievements, trust, projects, crm, packages, business, pricing, community, ai` — `api/src/lib/features.ts:10-22`), and the app calls none of them. So the app cannot hide a gated area, and the release-1.1 areas would have no way to disappear when switched off. | no `features` endpoint in `data/network/ServixApiService.kt` or anywhere in `data/` (grep) |
| F2 | **Role is decorative.** A `professional` and a `customer` get the *same* Home and the *same* Account; role is only printed as a badge. That is why work-side tools look "missing" to a professional and hire-side blocks look like clutter. | role used once, as a label: `ui/account/AccountHubScreen.kt:219`; no `role` branch in `ui/home/HomeTabScreen.kt` |
| F3 | **Two features are built but unreachable, one screen is dead.** Payments history has a full data layer and a sheet with **no entry point**; the professional "tools are on the website" card is rendered only inside an account screen that **nothing calls**. | `AccountRepository.getPayments()` (`AccountRepository.kt:72`), `AccountPaymentsSheetContent` (`AccountSheets.kt:157`) — zero call sites; `private fun Stage1AccountTab` (`ServixAppShell.kt:1039`) — zero call sites, holds `ProfessionalToolsWebCard` (`:1159`) |

Two smaller, repeated patterns: **duplicate entry points** (Notifications has a bell *and* an Account row; Servix AI
has a tab *and* a Home card) and **web-residue copy** ("Manage account on the website", the account-deletion
paragraph, the "pro tools live on the website" body text).

---

## 2. Inventory — every screen and every item

Legend for **Who sees it today**: **G** guest (signed out), **C** customer, **P** professional, **A** admin.
`Keep` = stays as is · `Move` = same content, new place · `Remove` = deleted · `Add` = new in 1.1.

### Bottom bar — `ui/navigation/ServixAppShell.kt:141-190`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Home | G C P A | Keep | Primary surface for everyone. |
| Explore | G C P A | Keep | Only browse entry point for guests; still valid for signed-in. |
| Bookings | G C P A | Keep | Move to slot 4; the work-side list is the professional's "Your jobs". |
| **Servix AI** | G C P A | **Move** | Leaves the bar; becomes the **pinned first row inside Chats** plus a Home shortcut (plan §3). An AI tab next to Bookings implies a fifth equal product area — the web treats the assistant as a conversation, not a destination. |
| Account | G C P A | Keep | Stays last. |
| **Chats (centre)** | G C P A | **Add** | New centre item, visually strongest, unread badge = Σ `unreadCount` over `GET /community/threads` (`api/docs/CHAT.md:41`); hidden entirely when `features.community` is false. |

### Home — `ui/home/HomeTabScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Greeting + date | C P | Keep | Orientation, one line. |
| Bell + unread dot (`notifications_open`) | C P | Keep | The single notification entry point (the Account row is removed, see below). |
| Avatar (36 dp) | C P | Keep | Fixed in 1.0.1 (profile photos). |
| "Sign in to book and track jobs" card | G | Keep | The guest's only conversion moment. |
| "Needs your attention" + attention cards | C P | Keep | Only renders when something genuinely needs action (email verify, payment pending). |
| "Your bookings" carousel / empty state | C P | Keep (C) · **Move** (P → "Your jobs") | Customers book; professionals fulfil. Same data, honest label. |
| "Browse by category" chips | G C A | Keep (G C) · **Remove** (P) | Hire-side browsing is noise on a professional's Home; the Explore tab remains one tap away. |
| "Recommended for you" list | G C A | Keep (G C) · **Remove** (P) | Same reason. |
| Servix AI card (bottom) | C P | **Move** | Replaced by the **Chats card**, which carries the AI inside it ("Servix AI is here"). Two AI entry points on one screen was the duplicate the plan calls out. |
| — | C | **Add** Chat card ("3 unread · Servix AI is here") | Chats is the new centre tab; Home needs one glance-and-go row. |
| — | C | **Add** "Post a request" card | `POST /requests` exists (`api/src/routes/requests.ts`) and `features.requests` gates it; today the app has no way to reach it. |
| — | C | **Add** "My network" row (when `features.community`) | `GET /community/connections`. |
| — | P | **Add** "Requests" card (when `features.requests`) | `GET /requests/browse` — the work-side job board. |
| — | P | **Add** "Network" card (when `features.community`) | Incoming connections with Accept/Decline. |

### Explore — `ui/explore/ExploreScreen.kt`, `ui/catalogue/SearchScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Search field | G C P A | Keep | — |
| Professionals / Services segmented control | G C P A | Keep | — |
| Filters sheet (category, city, rating, price, availability) | G C P A | Keep | Matches `GET /professionals` query params. |
| Sort | G C P A | Keep | — |
| Result count + list | G C P A | Keep | — |
| Empty state + "Clear filters" | G C P A | Keep | — |

### Professional profile — `ui/catalogue/ProfessionalProfileScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Avatar, name, title, rating, review count, verified | G C P A | Keep | — |
| Starting price + "Book" | G C A | Keep (G C) · **Remove** (P) | A professional cannot book themselves; the CTA dead-ends. |
| Save / Saved toggle | C P A | Keep | `POST/DELETE /account/saved/:slug`. |
| Escrow explainer paragraph | G C A | Keep | One line, legally useful. |
| Full "view profile" → services list | G C P A | Keep | — |

### Service detail — `ui/catalogue/ServiceDetailScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Service summary, price, duration | G C P A | Keep | — |
| "Servix protection" block | G C A | Keep (G C) · **Remove** (P) | Buyer-side reassurance; a professional is the seller. |
| Slot picker + notes | G C A | Keep | Booking sheet, `GET /services/:slug/availability`. |
| "Book & Pay" / "Request Booking" | G C A | Keep | Payment polling rules unchanged (Stage-3 gate). |

### Bookings — `ui/bookings/BookingsScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Title "My Bookings" | C P | Keep (C) · **Move** label → "Your jobs" (P) | Same list, honest title. |
| Active / Past segmented control | C P | Keep | `status` grouping already implemented. |
| Booking cards (status, amount, title, pro, date) | C P | Keep | — |
| Empty state + "Explore Services" | C P | Keep (C) · **Remove** CTA (P) | A professional searching for services to buy is not the intent of an empty work list. |
| "Sign in to see your bookings" | G | Keep | — |

### Booking detail — `ui/bookings/BookingDetailScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Status timeline | C P | Keep | Stage-3 gates untouched. |
| Payment pending banner + `Pay` + polling | C | Keep | Owner requirement: polling and status gates unchanged. |
| "Confirm Completion" + 3-day notice | C | Keep | — |
| "Cancel Booking" | C P | Keep | — |
| "Report a problem" (dispute) | C P | Keep | — |
| "Publish Review" | C | Keep | — |
| "View profile" | C P | Keep | — |
| — | C P | **Add** "Message" (opens/creates the thread: `POST /community/threads {bookingId}`) | Chat parity with the web; the booking is the natural start of a conversation. |

### Servix AI — `ui/ai/AiScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Chat transcript + composer | C P A | **Move** | Becomes the pinned first row inside Chats (`features.ai`), same UI. |
| Starter chips, feedback (Helpful / Not helpful) | C P A | **Move** | Same screen, new host. |
| Plan/usage footer ("AI 12/40 words this month") | C P A | Keep | Honest quota display; `GET /me/entitlements`. |
| Disclaimer ("AI suggestions are drafts…") | C P A | Keep | Required copy, one line. |
| "Sign in to use Servix AI" | G | Keep | Guest state inside Chats. |

### Account — `ui/account/AccountHubScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Header (avatar, name, email, role badge, verify CTA) | C P A | Keep | — |
| Profile | C P A | Keep | `PATCH /account/profile` works in-app. |
| Saved | C P A | Keep | `GET /account/saved` works in-app. |
| **Notifications** row | C P A | **Remove** | Duplicate of the top-bar bell (`ServixAppShell`), which is always visible. |
| Plan | C P A | Keep | `GET /me/entitlements` + `/billing/plan`. |
| Security | C P A | Keep | `GET /account/security-summary`. |
| This device | C P A | Keep | `GET /account/device` — real value (sessions, app version). |
| Updates | C P A | Keep | Self-update channel. |
| "Privacy and terms" | C P A | **Move** → single "Terms and privacy" row opening both documents | Plan §2 asks for one legal row, not a panel that lists them again. |
| Sign out | C P A | Keep | — |
| Deleting-account paragraph | C P A | **Move** into the Legal panel | It is a legal/process note, not an account action. |
| "Manage account on the website" | C P A | **Remove** | The app can do profile, security and plan itself; the row exists only because the web has a settings page. |
| — | C P A | **Add** Chats row ("3 unread · Servix AI inside") | Entry point into the new tab that also shows the badge when the bar is out of sight (e.g. scrolled). |
| — | C | **Add** My network row (`features.community`) | `GET /community/connections`. |
| — | C P A | **Add** Payments row | The data layer and sheet already exist and are unreachable (F3). `GET /account/payments`. |
| — | P | **Add** Work group: Requests, Network, Professional tools | Requests = `GET /requests/browse`; Network = connections; "Professional tools" replaces the dead `ProfessionalToolsWebCard` with one honest row (service setup really is web-only). |
| — | G | Keep (sign in / create account + legal) | Unchanged. |

### Notifications — `ui/notifications/NotificationsScreen.kt`

| Item | Who | Verdict | Why |
|---|---|---|---|
| List + timestamps + unread styling | C P A | Keep | Reached from the bell only. |
| "Mark all read" | C P A | Keep | `POST /account/notifications/read`. |
| Empty state | C P A | Keep | — |

### Auth, welcome, security — `ui/auth/*`

| Item | Who | Verdict | Why |
|---|---|---|---|
| Welcome (headline, subtext, Create account, Sign in, Browse first) | G | Keep | — |
| Provider sign-in (Google, GitHub) | G | Keep | Custom Tab hand-off (`docs/NATIVE_CLIENTS.md`). |
| Register (fields, terms checkbox, 18+) | G | Keep | — |
| Forgot password, OTP verify, security check / enroll / reset | G | Keep | Web-only fallback text stays (one string) because those steps genuinely are not in the app. |

### Not in the app at all (the "missing" half)

| Area | Who | Verdict | Why |
|---|---|---|---|
| Chats (thread list, conversation, media, voice) | C P | **Add** (Stage 1-2) | `api/docs/CHAT.md`. |
| My network (connections) | C P | **Add** (Stage 3) | `GET /community/connections`. |
| Requests / proposals (job board) | C P | **Add** (Stage 3) | `api/docs/MARKETPLACE_REQUESTS.md`, `api/docs/PROPOSALS.md`. |
| Payments history (read-only) | C P | **Add** (Stage 0 wiring) | already built, unreachable (F3). |
| Saved professionals | C P | Keep (exists) | — |
| Servix AI inside Chats | C P | **Add** (Stage 2) | `POST /ai/assistant/stream`. |
| `GET /features` gating | everyone | **Add** (Stage 0/1) | F1 — without it, no gated area can be hidden. |

---

## 3. The new bottom bar

`Home · Explore · 💬 Chats · Bookings · Account` — Chats in the centre and visually the strongest:

- **Chats** keeps a filled chat glyph on a 34 dp `SoftSand` disc when idle, and a `DeepForest` disc with an ivory
  glyph when selected. It is the only tab allowed to be `prominent` (enforced by a single flag on the item).
- **Unread pill** in `Coral`, `9+` capped, from Σ `unreadCount`, refreshed by the event stream (Stage 1).
- When `features.community` is false the bar renders **four** items — no empty tab, no "coming soon" (plan §2).
- **Guests get three items — `Home · Explore · Account`.** Plan §2 says guests see *"Home (browse), Explore and Sign in
  only"*; a Bookings or Chats tab for a signed-out visitor is a tab that can only show a sign-in wall. This is the one
  place where the 5-item rule and the guest rule disagree, so it is called out as decision 6 below.
- Notifications stay behind the bell; Servix AI leaves the bar.

Rendered comparison: `docs/ia/screens/ia_00_bottom_bar_before.png` vs `ia_01_bottom_bar_after.png`.
The bar is now a real component (`ui/navigation/ServixBottomBar.kt`) rather than inline shell code, so the
prototype renders **production pixels** — the extraction changes no visual, and the shell still owns all
navigation logic.

---

## 4. Before / after

| Screen | Before | After | Removals, one line each |
|---|---|---|---|
| Home — customer | `docs/screenshots/03_home_signed_in.png` | `docs/ia/screens/ia_04_home_customer_after.png` | Servix AI card → folded into the Chats card; nothing else lost. Additions: Chats, Post a request. |
| Home — professional | `docs/ia/screens/ia_02_home_professional_before.png` | `docs/ia/screens/ia_05_home_professional_after.png` | Category chips and "Recommended for you" — hiring-side content a professional does not act on. Additions: Requests, Network, "Your jobs" label. |
| Home — guest | `docs/screenshots/04_home_guest.png` | `docs/ia/screens/ia_06_home_guest_after.png` | Nothing removed; the bar drops to three items (Home · Explore · Account), and Chats, the bell and the bookings block are not rendered at all — a guest has no threads and no bookings. |
| Account — customer | `docs/screenshots/15_account.png` | `docs/ia/screens/ia_07_account_customer_after.png` | Notifications row (bell already does it), "Manage account on the website" (the app does it), deletion paragraph (moved into Legal), "Privacy and terms" panel → one row. Additions: Chats, My network, Payments. |
| Account — professional | `docs/ia/screens/ia_03_account_professional_before.png` | `docs/ia/screens/ia_08_account_professional_after.png` | Same removals, plus the dead "pro tools" card is replaced by one honest Work row. Additions: Requests, Network. |

The two professional "before" renders are new **because the state did not exist in the screenshot set**: the app has
never rendered a role-specific Home or Account. They use the shipping composables with a professional fixture, so
they are genuine before-pixels, not mock-ups.

---

## 5. Rules and skills applied

Plan rules, quoted and applied:

- *"Show only what the signed-in person can act on"* → every row above whose **Why** is "a professional does not act
  on this" (Home chips/recommended, Bookings empty CTA, profile Book CTA, Servix protection).
- *"Feature-flagged areas are hidden when the flag is false — no empty tabs, no 'coming soon' rows"* → Chats/Requests/
  Network rows exist only when their flag is true; the bar drops to four items. (Requires F1 to be fixed first.)
- *"remove duplicate entry points"* → Notifications row, second AI entry point.
- *"remove 'manage on the website' rows where the app can do the job itself"* → one row removed; the one genuine
  web-only area keeps exactly one row.
- *"keep a single 'Legal' row that opens Terms and Privacy"* → Account → Legal.

Skill rules quoted in the design of the artefacts:

- **Emil Kowalski Design Skills** — *one primary action per surface, and hierarchy must be unambiguous*: exactly one
  tab may be `prominent`, exactly one card per role may be the "first thing I came for" (Requests for a professional,
  Chats/next booking for a customer). No new colours, no shadows, no gradients — the disc is a tonal fill.
- **Impeccable Design Skills** — *remove before you add*: four removals are pure deletions (Notifications row, website
  row, deletion paragraph, second AI entry point); nothing was added to "balance" a screen.
- **UI UX Pro Max** — *recognition over recall, destructive/irreversible actions behind a confirmation*: the bar keeps
  five recognisable labels (never icon-only); "Delete account" stays a row inside Legal and still routes to support,
  because the API has no in-app deletion endpoint.
- **Anthropic Frontend Design** — *copy says what the thing does, in the user's words*: "Your jobs" for professionals,
  "3 unread · Servix AI is here", "Every charge and refund, read-only". No marketing adjectives.
- **Visual App Skills** — *brand tokens only*: Deep Forest / Forest Green / Warm Ivory / Soft Sand / Coral, radii
  16/24/12, Inter, no gradients; the bar, cards and rows reuse the existing components.
- **Android Testing Skills** — *render the real component, assert on pixels*: the prototype set renders the shipping
  `ServixBottomBar` and the shipping `AccountRow`/`AccountGroup`, so the before/after comparison cannot drift from the
  components; renders are produced by the same Robolectric harness at 50 % scale.
- **Superpowers** — *smallest reversible step*: the only production change in Stage 0 is a mechanical extraction
  (`ServixBottomBar.kt`) plus widening two composables to `internal`; no navigation, no new routes, no API calls.
- **Vercel Agent Skills** — *ship the artefact the reviewer needs*: audit + render set in one PR, images versioned next
  to the doc, CI green; deploy/Play steps deliberately untouched.

---

## 6. Honest status

**Verified**
- The audit table is derived from the code on this branch (string resources + composable structure) and from the
  screenshot set CI produces, not from memory.
- The bottom-bar extraction renders the same bar as the shell (compare `ia_00` with the app's own bar in any existing
  render — the bar is not visible in the numbered renders because each screen is rendered standalone, so `ia_00` is
  the reference).
- The prototype screens compile and render in the Robolectric pipeline; the images in `docs/ia/screens/` come from CI.

**Not verified**
- **Nothing has been exercised against the live API.** This sandbox cannot reach `api.servix.name.ng`, so every
  statement about endpoints is a reading of the specs (`api/docs/CHAT.md`, `MARKETPLACE_REQUESTS.md`,
  `PROPOSALS.md`, `NATIVE_CLIENTS.md`) and the route files, not a live call.
- **The feature flags' current server values are unknown to me** (`features.community`, `features.requests`,
  `features.ai`). The prototypes show the flags *on*; if `community` is off on the live host, Stage 1 is invisible
  in production until the owner flips it.
- **Role-specific content is a prototype, not behaviour.** No production screen branches on role yet (that lands with
  the approved arrangement).
- **The after-screens are static**: no scrolling states, empty states or long-name overflow were rendered for them.
  Loading/error/empty states for the new areas will be designed with their stage, not guessed now.

## 7. Decisions the owner should confirm

1. **Chats card vs booking card first** on a customer's Home — I propose: needs-attention, Chats, your bookings.
2. **"Post a request" on Home** for customers — show it always (when `features.requests`), or only when they have no
   active booking? I propose always, one card, below bookings.
3. **Payments as a row inside Account** (not a tab) — the data layer already exists, so this is wiring only.
4. **Professional Home drops category browsing entirely** — confirm, since it is the one content deletion that a
   professional might miss.
5. **Delete account** stays an email-support row (no in-app deletion endpoint exists in the API).
6. **Guest bottom bar: three items (Home · Explore · Account)** rather than five with two sign-in walls. Say if you
   would rather keep five for everyone.

**STOP.** No feature code is written; Stage 1 begins only on an explicit "go".
