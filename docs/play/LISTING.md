# Servix — Play Console listing and declarations (Block 6)

Copy and answers only: **nothing here was submitted to Play.** Paste the text fields into the
console, and use the tables as the answer key for the Data safety form, the content-rating
questionnaire and Target audience.

App name, tagline and body copy are the website's own wording, taken from
`repos/toluenetech-spec/servix` at ref `arena/01a0dc4c-servix` (`index.html`, `src/content/legal.js`,
`src/content/social.js`, `src/pages/HomePage.jsx`, `src/pages/HowItWorksPage.jsx`,
`src/pages/AboutPage.jsx`). Those files are byte-identical to the snapshot in
`reference/web/` (checked with `cmp`), so every quote below can also be read locally.

---

## 1. Store listing fields

| Field | Value | Limit |
| --- | --- | --- |
| App name | **Servix** | 30 — uses 6 |
| Category | **Business** | — |
| Tags | Business, Productivity | — |
| Contact email | **toluenetech@gmail.com** | — |
| Privacy policy | `https://www.servix.name.ng/privacy` | — |
| Terms of service | `https://www.servix.name.ng/terms` | — |
| Website | `https://www.servix.name.ng` | — |
| Default language | English (Nigeria); pick the closest English locale if the console does not list it | — |
| Short description | see §2 — **63 characters** | 80 |
| Full description | see §3 — **2389 characters** | 4000 |

Contact email provenance: `src/content/legal.js:3` — `export const LEGAL_CONTACT = 'toluenetech@gmail.com';`
It is the address the Privacy Policy gives for access, deletion and complaints ("Email
toluenetech@gmail.com, preferably from your account address") and the one the Terms give for support
and complaints. The policy is dated `28 September 2026`, version `2026-09-28` (`legal.js:1-2`).

The app itself links to both legal pages in-app: `ui/auth/AuthScreens.kt:667` (terms) and `:681`
(privacy) at registration, and `ui/account/AccountHubScreen.kt:555` (privacy) and `:561` (terms) in
the account hub. The URLs are absolute (`https://www.servix.name.ng/...`), so they match this listing.

## 2. Short description (≤ 80 characters)

```text
Professional services, simplified — discover, compare and book.
```

Built from two pieces of the site's own copy: the `<title>`/hero "Professional Services, Simplified"
(`index.html:8`, `HomePage.jsx:196`) and the og:image alt text "Discover. Compare. Book."
(`index.html:21`). No superlatives, no feature that the build does not have.

## 3. Full description (≤ 4000 characters)

```text
Professional services, simplified.

Servix connects customers with trusted professional service providers. Discover services, compare professionals, and manage bookings in one place.

DISCOVER
Find the right service or professional for what you need. Search by service or browse categories — every listing shows clear pricing, scope and delivery expectations.

COMPARE
Review portfolios, ratings, reviews and availability before you decide.

BOOK
Choose a date and time that suits you, confirm the scope, and send your booking request.

COMPLETE
Follow the booking status through Servix until the service is delivered: pay for an accepted booking, confirm completion, or report a problem if something is wrong.

REVIEW
Share honest feedback after a booking — it helps the next customer and rewards good professionals.

IN THE APP
• Search and category filters across professional services
• Professional profiles with skills, services, portfolios, ratings and reviews
• Service pages with availability and transparent pricing
• Booking requests and a status timeline you can follow
• Payment through Paystack's hosted checkout, which opens in your browser — Servix never asks you to type card details into the app
• Booking and payment notifications, delivered by a periodic check rather than push
• Servix AI, an in-app assistant that drafts and explains, clearly labelled as drafts to check
• Account hub for your profile, saved professionals, plan and security settings

WHY SERVIX
Trust before transactions. Professional work deserves professional presentation, and finding, comparing and booking a service should be simple.

GOOD TO KNOW
• Servix accounts and bookings are for people aged 18 or over.
• Professionals, not Servix, perform the booked work. Listings, ratings and verification labels do not guarantee the quality, safety or suitability of a professional — check qualifications appropriate to your work.
• Notifications are not instant: the app checks for updates periodically, so a notification can lag by up to about 15 minutes.
• There is no in-app chat, community feed or social network in this app. Plan, security and other account settings are managed on the website.
• Servix is operated by Toluwalase O. Samuel in Nigeria as an individual operator.

Privacy: https://www.servix.name.ng/privacy
Terms: https://www.servix.name.ng/terms
Contact: toluenetech@gmail.com
```

### Where each block comes from

| Block | Source |
| --- | --- |
| "Servix connects customers with trusted professional service providers…" | `index.html:9-12` (meta description), `HomePage.jsx:197-198` |
| DISCOVER / COMPARE / BOOK / COMPLETE / REVIEW | `HowItWorksPage.jsx:11-32` (customer steps, lightly compressed to fit the character budget) |
| "Trust before transactions…" | `AboutPage.jsx:8-9` (first value) |
| "Professional work deserves professional presentation…" | `AboutPage.jsx:12-13` |
| "…finding, comparing and booking a service should be simple" | `AboutPage.jsx:16-17` |
| 18+ | `legal.js` Terms "Eligibility and accounts" ("You must be at least 18"); Privacy "Children and safety" ("intended for people aged 18 or over") |
| "Professionals, not Servix, perform the booked work…" | `legal.js` Terms "What Servix provides" |
| Operated by an individual operator | `legal.js` Terms "Operator and agreement" |
| Polling latency | app code: `notifications/NotificationsWorker.kt:15-16` |

### Deliberately absent from the copy

These exist on the web platform but **not** in this build, so the listing must not promise them:

| Not claimed | Why |
| --- | --- |
| Community, Messages, Network / connections | No such screens or calls in the app. `grep -ril "community\|conversation\|inbox"` over `app/src/main/java` hits only AI chat, auth and notifications. The Privacy Policy mentions messages only "where community features are enabled" (`legal.js`, "Private messages and connections"). |
| Instant / push notifications | The app polls `GET /account/notifications` every 15 minutes (`NotificationsWorker.kt:15-16`); there is no FCM dependency. |
| "Escrow" | The Terms explicitly decline that framing: "We do not describe held booking balances as a bank deposit or regulated escrow product" (`legal.js`, "Prices, fees and payment testing"). |
| Guaranteed quality or "verified" quality | Terms: listings, ratings and verification labels "do not guarantee the quality, safety, licensing or suitability of a professional". |
| Live payment guarantees | The same Terms section says payment integrations "may be in test mode". The copy therefore describes the mechanism (Paystack hosted checkout in a browser), not an outcome. |
| Demo statistics ("500+ professionals", "4.8/5") | Those are demo display values in the site's `TRUST_STATS` (`HomePage.jsx:28-32`) until the API returns live numbers. Not usable in a store listing. |
| 8 service categories | Same demo block; the app renders whatever `GET /categories` returns (`ServixApiService.kt:179`). |

## 4. Data safety — collection and sharing

Every answer below is what the **code does**, with the file and line that decides it. "Collected"
means transmitted off the device to Servix's API; on-device-only processing is marked not collected,
which is what Play's definition requires.

| Play data type | Collected? | Shared? | Optional? | Purpose | Evidence |
| --- | --- | --- | --- | --- | --- |
| Name | **Yes** | No | Required | Account management | `data/model/AuthModels.kt:38` (`fullName` on `UserDto`), `:52` (`RegisterRequest.fullName`); editable via `AccountModels.kt:126-129` (`UpdateProfileRequest`) |
| Email address | **Yes** | No | Required | Account management, sign-in, verification | `AuthModels.kt:37`, `:53`; `ServixApiService.kt:112-118` (`auth/register`, `auth/login`), `:130-133` (email verification) |
| Phone number | **No** | — | — | — | No phone field exists anywhere in the app: `grep -ri phone` over `app/src/main/java` returns only the location rationale string, an icon name and a comment in `ui/theme/Type.kt` |
| User IDs | **Yes** | No | Required | Account management | `AuthModels.kt:36` (`UserDto.id`, the account UUID) |
| Address | **No** | — | — | — | No address field in any DTO; no shipping/billing address form |
| Approximate location | **No** | — | Optional | App functionality, **on-device only** | `AndroidManifest.xml:5` (coarse permission); `location/LocationButton.kt:38-42` — a DataStore key `approximate_city`, and the KDoc: "One user-initiated coarse fix. No coordinate is persisted or passed to any app networking code." `:47`, `:54`, `:71` use `NETWORK_PROVIDER` (coarse). The rationale the user sees: "Servix uses your approximate location to show professionals near you. It stays on your phone." (`:122`) |
| Precise location | **No** | — | — | — | Only `ACCESS_COARSE_LOCATION` is declared (`AndroidManifest.xml:5`); no `ACCESS_FINE_LOCATION` |
| Photos | **Yes** | No | Optional | App functionality (avatar) | `ui/account/AccountHubScreen.kt:282-303` (`PickVisualMedia` → `POST /uploads?kind=avatar` → `PATCH /account/profile`), `data/repository/AccountRepository.kt:98-103`; `ServixApiService.kt:90` (`uploads`) |
| Financial info (card, bank details) | **No** | — | — | — | The app never renders a card field. Payment opens Paystack's hosted checkout in a browser: `ui/bookings/BookingDetailScreen.kt:280-290` (`initiatePayNow` passes `authorizationUrl`), `:1047` (`CustomTabLauncher.launchUrl(context, url)`); `ServixApiService.kt:270` starts the "Bookings & Paystack Payment" section |
| Purchase history | **Yes** | No | Required | App functionality | Booking amounts and statuses are read back from the API (`BookingModels.kt`, `ServixApiService.kt:270-311`) — Servix's own records of your bookings, not card data |
| Other user-generated content (reviews: rating + text) | **Yes** | No | Optional | App functionality; **visible to other users** | `BookingModels.kt:50` (`SubmitReviewRequest`), `data/repository/BookingRepository.kt:107-117`, `ServixApiService.kt:308-311`; other reviews are read publicly for a professional/service: `:209`, `:239` |
| Other user-generated content (Servix AI prompts) | **Yes** | No | Optional | App functionality | `ServixApiService.kt:62` (`POST ai/assistant`); the AI routes state "Nothing here writes to the database", only metering: `reference/api-routes/ai.ts:5-6` |
| Device or other IDs | **Yes** | No | Required | App functionality + security | Every request carries `User-Agent: ServixAndroid/<version> (Android <release>; <manufacturer> <model>)` and `X-Servix-Client: ng.name.servix.app` — `data/network/ClientUserAgent.kt:8`, `:12`, `:15`. No advertising ID, no hardware serial, no IMEI: `grep -i "advertisingId\|AdvertisingIdClient"` finds nothing. The API echoes what it saw back for the "this device" card: `ServixApiService.kt:107` (`GET account/device`), `AccountModels.kt:113-122` (`ip`, `device`, `os`, `client`, `userAgent`, `seenAt`) |
| App interactions / analytics | **No** | — | — | — | No analytics SDK in `app/build.gradle.kts:100-121` (androidx, OkHttp/Retrofit, kotlinx-serialization, Coil only). The website's optional Vercel measurement is web-only and consent-gated (`legal.js`, "Cookies and optional measurement") |
| Crash logs / diagnostics | **No** | — | — | — | No Crashlytics/Sentry/Bugsnag dependency; no crash-upload call |
| Contacts, calendar, SMS, call logs, health, browsing history | **No** | — | — | — | No such permission in `AndroidManifest.xml:3-6` (INTERNET, ACCESS_NETWORK_STATE, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS only) |

**Third-party sharing: none.** No data is passed to a third party for its own purposes, and there is
no advertising or analytics SDK (§4, last rows). Servix's own processors — hosting, database, uploads,
email, Paystack for payments — are named in the Privacy Policy ("Who receives information"), with "We
do not sell personal information or use it for cross-site advertising." Under Play's definition,
processors acting on Servix's instructions are not "sharing", which is why every row above says No.

## 5. Data safety — security practices

| Question | Answer | Evidence |
| --- | --- | --- |
| Is data encrypted in transit? | **Yes** | `res/xml/network_security_config.xml:4-8` — `cleartextTrafficPermitted="false"`, system trust anchors only; `AndroidManifest.xml:13` — `android:usesCleartextTraffic="false"`; `:14` — that config is applied |
| Is data encrypted at rest on the device? | **Yes** (tokens) | Session tokens live in `EncryptedSharedPreferences` with an AES-256-GCM master key: `data/local/TokenStore.kt:5-6`, `:58-61`. Separately, `android:allowBackup="false"` (`AndroidManifest.xml:8`) keeps app data out of cloud backup |
| Has the app had an independent security review? | **No** | No third-party assessment has been performed. Answer honestly; the code-level controls (HTTPS-only, encrypted token store, TOTP/passkey flows at `auth/security/*`) are not a substitute for an audit |
| Do you provide a way to request data deletion? | **Yes** | See §6 |
| Committed to Play's Families policy? | **Not applicable** | 18+ (§8) |

## 6. Data deletion (Play's "Data deletion" section)

* **In-app deletion: no.** The app has no delete-account flow — `grep -ri "delete account"` over
  `app/src/main/java` returns nothing. Account management panels open the website:
  `ui/account/AccountHubScreen.kt:170` (`/dashboard/settings`), `:441` (`/dashboard/plan`), `:523`
  (`/dashboard/security`).
* **Request route: email.** `toluenetech@gmail.com` (`legal.js:3`), which the Privacy Policy designates
  for "access, correction, deletion, restriction or portability" requests and explains what can be
  removed and what may lawfully be retained ("Retention and deletion", "Your choices and rights").
  The policy is explicit that completion is not instant — "We do not promise instant deletion from
  every system" — because some work is manual and backups follow the host's cycle.
* **URL to give Play for a deletion request:** `https://www.servix.name.ng/privacy` (it contains both
  the deletion route and the address).
* Also available: complaint to the Nigeria Data Protection Commission, `ndpc.gov.ng` (`legal.js`,
  "Your choices and rights").

## 7. Content rating questionnaire (IARC)

| Question group | Answer | Why |
| --- | --- | --- |
| Violence, blood, injury | **No** | No violent content in any screen or copy |
| Sexuality, nudity, suggestive content | **No** | — |
| Profanity, crude humour | **No** | App copy is service/marketplace language |
| Drugs, alcohol, tobacco | **No** | — |
| Gambling (real or simulated) | **No** | No wagering, no simulated casino, no loot boxes |
| Horror, fear | **No** | — |
| Discrimination, hate | **No** | Terms prohibit unlawful or discriminatory material (`legal.js`, "Acceptable use and content") |
| User-generated content | **Yes, limited to reviews** | Customers post a star rating and text after a booking (`BookingModels.kt:50`, `BookingRepository.kt:107-117`), and reviews are shown to other users (`ServixApiService.kt:209`, `:239`) |
| Can users communicate directly with each other? | **No** | No chat, DM, comment thread or community feed exists in the app (see §3). The only "report" surface is a private dispute on your own booking: `ui/bookings/BookingDetailScreen.kt:1213` ("Report a problem") |
| Does the app share the user's current location with other users? | **No** | Approximate city stays on the device (`LocationButton.kt:42`) |
| Does the app allow purchases? | **Yes** | Real-world professional services paid through Paystack's hosted checkout in a browser (`BookingDetailScreen.kt:1047`) |
| Does the app contain ads? | **No** | No ad SDK (§4); no ad units in any screen |
| Digital goods / loot boxes / crypto | **No** | The app sells no digital items and handles no cryptocurrency |
| Miscellaneous (dating, medical, legal advice) | **No** | Servix AI gives drafts and explanations, not professional advice — the in-app disclaimer reads "AI suggestions are drafts — check them before you rely on them. Servix AI never moves money or changes your bookings." (`res/values/strings.xml:7`, `ai_disclaimer`) |

Expected outcome: a low-impact rating — every content question is No, there are no ads, and the only
user-generated content is a post-booking star rating plus review text. Take the console's own result
as authoritative.

**Payments note for the reviewer:** services booked through Servix are real-world professional
services, not digital goods, so the Paystack browser checkout is consistent with Play's payments
policy (Play Billing is not required for real-world services).

## 8. Target audience

| Field | Answer | Evidence |
| --- | --- | --- |
| Age groups | **18 and over only** | Privacy "Children and safety": "Servix accounts and bookings are intended for people aged 18 or over"; Terms "Eligibility and accounts": "You must be at least 18 and able to enter a binding agreement" (`legal.js`) |
| Designed for families | **No** | Not enrolling in the Families programme |
| Ads aimed at children | **No** | No ads at all |
| Appeals to children | **No** | Marketplace/service-booking content, naira pricing, professional profiles |

The app has no age gate — the declaration is the control, matching the legal text. If Play asks for a
neutral-age screening answer, it is the lowest-sensitivity answer available, because there is no
content category in §7 above that applies.

## 9. Other console declarations (for completeness)

| Declaration | Answer |
| --- | --- |
| App access | App can be browsed without signing in; booking, reviews and AI need an account. The console asks for reviewer instructions + a demo account — supply a working demo login in the console itself, never in this repo. |
| Ads | No ads. |
| News app | No. |
| COVID-19 apps | No. |
| Data safety → "Is any of this data required?" | Name, email, user ID, purchase history, device info: required. Photos, reviews, AI prompts, location: optional/in-app choice. |
| Government app | No. |
| Financial features | No lending, banking or crypto. Payments for real-world services only. |
| Health | No. |
| Photo/video permissions | The app uses the system Photo Picker (`PickVisualMedia`, `AccountHubScreen.kt:284`), which grants access per item — no `READ_MEDIA_IMAGES` permission is declared. |

---

## Verification performed for this document

* Every cited file:line was read in this workspace at commit `a93e81f`, the head of this branch when the document was written.
* Website quotes were fetched from `repos/toluenetech-spec/servix` ref `arena/01a0dc4c-servix` with
  `Accept: application/vnd.github.raw`, and `legal.js` / `LegalPage.jsx` were `cmp`-identical to the
  in-repo snapshot under `reference/web/`.
* Absence claims (phone number, ads, analytics, chat, delete-account, fine location) were checked with
  `grep` across `app/src/main/java` and `app/src/main/AndroidManifest.xml`, not from memory.
* Description lengths were counted programmatically, not estimated.

**STOP.** Nothing was submitted to Play. Block 7 (`docs/play/RELEASE_CHECKLIST.md`) waits for "go".
