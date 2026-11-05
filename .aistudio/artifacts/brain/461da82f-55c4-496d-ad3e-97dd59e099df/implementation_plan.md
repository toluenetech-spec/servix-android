# Servix — Native Android Marketplace Client

Reconstruct **Servix** (`https://www.servix.name.ng`) as a native Android customer application in Kotlin and Jetpack Compose (Material 3) wired directly to the live Servix API (`https://api.servix.name.ng/api/v1/`). Customers can discover identity-verified professionals in Nigeria, book services at a locked price with escrow protection, chat with Servix AI, and manage their bookings and account.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and product choices incorporate your answers from Phase 1 and reconcile `SPEC.md` with the authoritative backend routes (`reference/api-routes/*.ts`):

- **Backend as Single Source of Truth**: Where `SPEC.md` summarizes or differs from `reference/api-routes/*.ts`, we follow the backend routes strictly without client-side fallbacks or guessed fields:
  - **Reviews (`POST /bookings/:id/review`)**: Sends `{ rating: 1..5, text?: string }` (`bookings.ts:41-44`).
  - **Save Professional (`POST /account/saved`)**: Sends `{ profileSlug: string }` (`account.ts:74`).
  - **Rebook Flow (`GET /bookings/:id/rebook` → `POST /bookings`)**: Calls `GET /bookings/:id/rebook` (`bookings.ts:138`) to load the sanitized prefill (active services + optional previous notes), navigates to the selected service's booking sheet, and passes `rebookedFrom` to `POST /bookings` (`bookings.ts:33`).
  - **Profile Edit (`PATCH /account/profile`)**: Sends `{ fullName?: string, avatarUrl?: string | null }` (`account.ts:16`).
  - **Notifications Read (`POST /account/notifications/read`)**: Sends `{ ids: [...] }` or `{ all: true }` (`account.ts:59`).
  - **Sort & Query Parameters**: Uses `price-asc` and `price-desc` (`professionals.ts:41-43`, `services.ts:37-39`), and supports `minRating`, `maxPrice`, `availability`, and `available` (`today` | `tomorrow` | `week`).
  - **Bookings Response (`GET /bookings`, `GET /bookings/:id`)**: Parses flattened fields (`serviceTitle`, `priceUnit`, `serviceId`, `professionalId`, `professionalName`, `customerName`, `hasReview`, `deliveredAt`, `completedAt`, `disputeReason`, `cancelReason`) directly from `serializeBooking` (`bookings.ts:69-91`).
- **Customer-Only Registration**: The Create Account screen registers customers without an account-type selector (`accountType` omitted so the backend defaults to `"customer"`). Includes the link *"Want to offer your services on Servix? Apply at www.servix.name.ng"* opening in a Chrome Custom Tab. Signed-in `professional` or `admin` users can use customer features and see a prominent card linking to `www.servix.name.ng` for professional/admin tools.
- **Servix AI Two-Tab Hub**: Mirrors `AiPage.jsx` for customers with two segmented tabs: **Assistant** (SSE streaming chat via `POST /ai/assistant/stream` with fallback to `POST /ai/assistant`, starter prompts, Copy, and Helpful? 👍/👎 feedback via `POST /ai/feedback`) and **Budget guide** (`POST /ai/pricing/guidance` with category picker from `GET /categories`, optional brief and delivery days, rendering real ₦ price ranges and stats).
- **Staged Delivery**: Execution proceeds one stage at a time (Stages 1 → 6), verifying compilation and live API behavior at each stage before awaiting your `"go"` for the next stage.

---

## 1. Overview & Core Concept

- **What It Does**: A native Android client for Servix that connects customers with verified Nigerian professionals across categories like Web Development, Graphic Design, Photography, Video Editing, Marketing, and Consulting.
- **Target Audience / Persona**: Customers in Nigeria seeking transparent scope, locked Naira (`₦`) pricing, escrow-backed payment protection via Paystack, and grounded AI assistance.
- **Key Value**: Brings the complete customer journey—discovery, comparison, slot-based booking, hosted Paystack checkout, escrow confirmation/dispute/review, and Servix AI—into a fast, accessible Material 3 Android experience.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Onboarding & Auth**: Welcome hero screen (*"Services, delivered professionally."*) → Sign in (`POST /auth/login`) or Create account (`POST /auth/register`) → Email verification (`GET /auth/verification-method` → 6-digit OTP via `POST /auth/verify-email/code` or link check + `POST /auth/verify-email/resend`) → Forgot password (`POST /auth/forgot-password`). Session auto-restores on launch via `GET /me` and single-flight `POST /auth/refresh`.
  2. **Catalogue Discovery**: Home screen with personalized greeting, search bar, category chips (`GET /categories`), *"Your next booking"* card (when active bookings exist), and *"Top-rated professionals"* (`GET /professionals?sort=rating`). Search tab with keyword, category, location, rating, starting price, availability window, and sort filters.
  3. **Profile & Service Inspection**: Professional profile (`GET /professionals/:slug`, `/services`, `/reviews`, `/availability/summary`) with Verified badge (only when `verified == true`), honest demonstration labels, Save/Unsave toggle, and tabs for About, Trust & Performance (when enabled), Services, Portfolio, and Reviews. Service detail (`GET /services/:slug`, `/reviews`) with image gallery, `₦` price, delivery days, included items, requirements, FAQs, and professional strip.
  4. **Booking & Escrow Checkout**: *"Request Booking"* bottom sheet fetches real 14-day slots (`GET /services/:slug/availability`), collects optional notes, displays verbatim escrow copy (*"The price is locked when you book. Payment is held securely and only released after you confirm completion."*), creates the booking (`POST /bookings` with `Idempotency-Key`), initializes payment (`POST /bookings/:id/pay`), launches `authorizationUrl` in a Chrome Custom Tab, and polls `GET /bookings/:id` every 2 seconds for up to 60 seconds on return.
  5. **Booking Lifecycle**: Bookings tab (Active / Past) → Booking detail showing reference (`SVX-…`), server status chip, timestamps in `Africa/Lagos`, amount, notes, AI Project Health & status explanation, and server-permitted actions: Pay, Cancel, Confirm Completion, Report a Problem (dispute ≥ 10 chars), Leave a Review (1–5 stars + text), and Book Again (`GET /bookings/:id/rebook`).
  6. **Servix AI**: Segmented switcher for **Assistant** (streaming SSE answers, Markdown rendering, Copy, 👍/👎 feedback with privacy notice on thumbs-down) and **Budget guide** (category price range lookup), plus monthly token usage meter (`GET /ai/usage`) and verbatim disclaimer.
  7. **Account Management**: Overview (`GET /account/overview`, `GET /account/insights`), Edit Profile (`PATCH /account/profile`), Saved Professionals (`GET/POST/DELETE/PATCH /account/saved`), Payments history (`GET /account/payments`), Notifications (`GET /account/notifications`, `/unread`, `POST /account/notifications/read`), read-only Plan & Entitlements (`GET /me/entitlements`), About section (version + `API: api.servix.name.ng`), Privacy/Terms links, and Sign out.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Editorial, calm, low-ornament, premium. Hierarchy driven by typography, generous spacing, and crisp `1.dp` Soft Sand borders—never gradients, neon, or heavy drop shadows.
  - *Color Palette & Mood*:
    - Primary / Headers / CTA: Deep Forest (`#12372A`) with Warm Ivory (`#F7F4EC`) text
    - Secondary / Selected / Positive: Forest Green (`#1F5C45`)
    - App Background: Warm Ivory (`#F7F4EC`)
    - Cards / Forms: Pure White (`#FFFFFF`)
    - Subtle Surfaces / Borders / Dividers: Soft Sand (`#EDE8DC`) & Border (`#DDD7C9`)
    - Accent / Alerts / Stars: Coral (`#E56B5D`, used sparingly)
    - Primary Text: Charcoal (`#202622`)
    - Secondary Text: Muted Gray (`#68716B`)
  - *Typography & Hierarchy*: Bundled **Inter** font family (`res/font/inter.ttf`) wired in `Type.kt`. Tight negative letter-spacing on bold display/headline styles, comfortable `16.sp` body text, and tabular Naira formatting (`₦350,000`).
  - *Component Styling & Layout*: Corner radii strictly `3.dp` (badges/tags), `6.dp` (buttons/inputs), and `10.dp` (cards/sheets). Minimum `48.dp` touch targets and full edge-to-edge inset handling.
- **Interactive Feedback & Motion**: Shimmering/pulsing skeleton placeholders on every network screen, calm inline error states with Retry buttons, persistent top offline banner via `ConnectivityManager`, and pull-to-refresh on list/detail screens.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Single-Flight Token Refresh & Persistent CookieJar**
  - *Chosen Approach*: Store the short-lived JWT access token in memory + `EncryptedSharedPreferences`, persist the `servix_refresh` `httpOnly` cookie in a custom SharedPreferences-backed `CookieJar` scoped to `/api/v1/auth`, and synchronize `401` retries inside an OkHttp `Authenticator` using a mutex lock.
  - *Why*: The backend rotates refresh tokens on every `/auth/refresh` call and revokes the entire session family if a token is replayed concurrently (`auth.ts:200-227`). Single-flight synchronization prevents accidental family revocation when parallel requests hit a `401`.
- **Decision 2: Native SSE Streaming with Transparent Non-Streaming Fallback**
  - *Chosen Approach*: Read `POST /ai/assistant/stream` line-by-line using OkHttp's streaming `ResponseBody` (`source().readUtf8Line()`), parsing `data: {...}` frames for `start`, `delta`, `done`, and `error` while balancing unclosed `**` and `` ` `` tokens during live Markdown rendering. Fall back to `POST /ai/assistant` on `404` or non-SSE responses.
  - *Why*: Matches `aiApi.js` and `AiBits.jsx` behavior so users see immediate first-token feedback without raw Markdown flicker.
- **Decision 3: Hosted Paystack Checkout via Chrome Custom Tabs + Bounded Polling**
  - *Chosen Approach*: Launch `authorizationUrl` in a Chrome Custom Tab (`CustomTabsIntent`) and, when `MainActivity` resumes, poll `GET /bookings/:id` every 2 seconds for up to 60 seconds until `status != "pending_payment"`.
  - *Why*: Card details never touch the app, and because browser redirects are never authoritative (`PAYMENTS.md §2`), polling waits for the signed Paystack webhook to transition the booking server-side.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:
```
┌───────────────────────────────────────────────────────────────────────────┐
│                         MainActivity (Edge-to-Edge)                       │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                     OfflineBanner + ServixAppShell                  │  │
│  │  ┌───────────┬───────────┬──────────────┬─────────────┬──────────┐  │  │
│  │  │   Home    │  Search   │   Bookings   │  Servix AI  │ Account  │  │  │
│  │  └─────┬─────┴─────┬─────┴──────┬───────┴──────┬──────┴────┬─────┘  │  │
│  └────────┼───────────┼────────────┼──────────────┼───────────┼────────┘  │
└───────────┼───────────┼────────────┼──────────────┼───────────┼───────────┘
            ▼           ▼            ▼              ▼           ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                 ViewModels (StateFlow + UiState<T> + Actions)             │
│   AuthVM · HomeVM · SearchVM · ProDetailVM · ServiceDetailVM ·            │
│   BookingsVM · BookingDetailVM · AiVM · AccountVM                         │
└───────────┬────────────────────────┬──────────────────────────┬───────────┘
            ▼                        ▼                          ▼
┌───────────────────────┐ ┌──────────────────────┐ ┌────────────────────────┐
│   Auth & Session      │ │  Catalogue & Booking │ │   AI & Account         │
│   Repository          │ │  Repository          │ │   Repository           │
└───────────┬───────────┘ └──────────┬───────────┘ └────────────┬───────────┘
            └────────────────────────┼──────────────────────────┘
                                     ▼
┌───────────────────────────────────────────────────────────────────────────┐
│              Network Layer (Retrofit + OkHttp + Serialization)            │
│  • AuthHeaderInterceptor (Bearer token)                                   │
│  • TokenRefreshAuthenticator (Single-flight POST /auth/refresh on 401)    │
│  • PersistentCookieJar (httpOnly servix_refresh cookie)                   │
│  • SseStreamClient (OkHttp streaming reader for /ai/assistant/stream)     │
│  • ApiErrorParser ({ error: { status, code, message, errors, meta } })    │
└───────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - `SessionManager`: Holds `StateFlow<AuthState>` (`Initializing`, `Unauthenticated`, `Authenticated(user, needsVerification)`), backed by `EncryptedSharedPreferences` for `accessToken` and cached user profile, plus `PersistentCookieJar` for the refresh cookie.
  - `UiState<T>`: Sealed interface (`Loading`, `Success(T)`, `Error(ApiError)`) driving consistent skeleton, empty, and retryable error views across all screens.
  - `Formatters`: Naira formatter (`₦` + `en-NG` thousands separators; divides `*Kobo` fields by 100) and `Africa/Lagos` (`ZoneId.of("Africa/Lagos")`) ISO-8601 date/time formatter.

- **Interactive Component & State Mapping**:
  - **Stage 1 (Foundation & Auth)**: `WelcomeScreen` ↔ `LoginScreen` ↔ `RegisterScreen` ↔ `VerifyEmailScreen` (auto-detects `otp` vs `link` via `GET /auth/verification-method`, 6-digit OTP input with 60s resend cooldown) ↔ `ForgotPasswordScreen`. Field-level error mapping from `error.errors`.
  - **Stage 2 (Catalogue)**: `HomeScreen` (greeting, search bar, category chips, next booking card, top-rated pros) → `SearchScreen` (debounced query, filter bottom sheet for category/location/rating/price/availability, sort menu, pagination) → `ProfessionalProfileScreen` (About, Trust, Services, Portfolio, Reviews tabs + Save toggle) → `ServiceDetailScreen` (gallery pager, FAQs accordion, requirements, pro strip).
  - **Stage 3 (Bookings & Payment)**: `BookingBottomSheet` (slot selector grouped by day from `/services/:slug/availability`, notes, verbatim escrow copy, idempotent `POST /bookings` + `POST /bookings/:id/pay` → Chrome Custom Tab → 2s polling) → `BookingsScreen` (Active / Past tabs) → `BookingDetailScreen` (status badge, timeline/dates, Cancel/Confirm/Dispute/Review modals, and Rebook prefill sheet).
  - **Stage 4 (Servix AI)**: `AiScreen` with `AssistantTab` (SSE stream reader, Stop button, starter prompt chips, light Markdown composable, Copy button, 👍/👎 feedback with comment form on 👎) and `BudgetGuideTab` (`POST /ai/pricing/guidance` with category dropdown, days, brief, and clamped ₦ range display), plus `AiUsageCard` (`GET /ai/usage`) and 403 quota upgrade banner.
  - **Stage 5 (Account)**: `AccountScreen` hub (`GET /account/overview`, role banner for pro/admin linking to `www.servix.name.ng`) → `SavedProfessionalsScreen` (preferred toggle, private notes, remove), `PaymentsScreen` (status filter, summary cards, booking links), `NotificationsScreen` (filter tabs, mark one/all read, cursor pagination), `PlanScreen` (`GET /me/entitlements` read-only usage meters + website link), `ProfileSettingsScreen` (`PATCH /account/profile`), and `AboutDialog`.
  - **Stage 6 (Polish & Release)**: Custom adaptive icon (`#12372A` Deep Forest background with Warm Ivory `"SX"` monogram), accessibility semantics (`48.dp` targets, content descriptions), ProGuard/R8 rules (`isMinifyEnabled = true`, `versionName = "1.0.0"`), and `CHANGELOG.md`.
