# Pass 3 (extended) — Explore, profile, service detail, provider sign-in, notifications

Version **0.6.3** / versionCode **12** · branch `arena/f657a17e-servix-android`
Release: `v0.6.3-b32` (APK, CI green) · signing cert SHA-256 `cf6460a5e679cd797779551af48b1bf9dd8dda8f524a14db55489ae49717299d`

Everything below builds through CI only (no local Gradle/Android SDK in the sandbox).

## (a) Plan §5 screens

| Screen | What changed |
| --- | --- |
| **Explore** (`ui/explore/ExploreScreen.kt`, new) | Search field pinned under a slim top bar; Professionals/Services **segmented control** (not tabs); a horizontally scrolling filter chip row (`Filters ·2`, Sort, Category, City, Available now); grouped filter sheet with pill chips, one ₦-labelled price slider and a sticky "Show N results" action; §3 empty state. The old `SearchScreen` composable was deleted; `SearchViewModel`/`SearchUiState` (now with `priceLimit` + `setPriceLimit`) are reused. |
| **Professional profile** | Root is `Box { LazyColumn + ProfileBookBar }`. Header: 72 dp avatar, name + verified badge, title, "location · Responds …", availability badge. Three compact stats (rating / reviews / projects). Sticky pill tab strip About · Services · Portfolio · Reviews. Folded trust panel into About. Sticky bottom bar: price "From ₦…" + escrow line, 52 dp Save icon button, primary **Book**. |
| **Service detail** | Edge-to-edge `HorizontalPager` (16:10) with page dots replaces the single image + thumbnail strip; provider row, price block, "What's included" checkmarks, date row → time pills, expandable FAQ rows, review avatars with initials, and a sticky bottom bar "Book this service · ₦…". |
| **Booking sheet** | 24 dp top radius, grab handle, title, price line, date row, time pills, notes field (12 dp radius), "Book & Pay", and "Payment is held securely…" as `bodySmall` **under** the button. |

Screenshots: `docs/screenshots/06_explore_results.png`, `07_explore_empty.png`, `08_professional_profile.png`, `09_service_detail.png`.

## (b) Google / GitHub sign-in (pure browser OAuth — no Play Services, no Firebase)

* Buttons appear **only** for providers the API reports: `GET /auth/oauth/config` → `{ providers }`, `Cache-Control: no-store` (`api/src/routes/oauth.ts:25-28`). While `AUTH_OAUTH_ENABLED=false` the list is empty and the buttons are not rendered at all.
* Tap → Chrome **Custom Tab** at `https://api.servix.name.ng/api/v1/auth/<provider>/native/start?client=ng.name.servix.app` (`oauth.ts:49-57`; `client` must be in `NATIVE_CLIENT_IDS`, otherwise 403 `ORIGIN_REJECTED`).
* Return path: manifest intent filter `ng.name.servix.app` / host `oauth` / path `/callback`, `launchMode="singleTask"`, handled in `onNewIntent` (`NATIVE_CLIENTS.md:46-63`) → `OAuthReturnBus` → `AuthViewModel.handleOAuthReturn`.
  * `?ticket=` → `POST /auth/oauth/native/exchange { ticket }` (`oauth.ts:85-90`, ticket 32–128 chars, rate limit 10 / 5 min) → the usual `{ security }` payload → existing **SecurityCheck** screen (`next = enroll` for a new account, `factor` when already enrolled).
  * `?error=cancelled` → silent return. `?error=failed` → snackbar. `?error=link_required` → "Sign in using your existing method first, then explicitly connect this provider." (`oauth.ts:71,79-80`).
* Terms: on **Create account** the provider buttons stay disabled until the Terms box is ticked, with a one-line hint ("Tick the terms below to continue with a provider.").
* Every request already carries `X-Servix-Client: ng.name.servix.app` (`NATIVE_CLIENTS.md:8-22`).

Screenshots: `docs/screenshots/01b_welcome_providers.png`, `02b_sign_in_providers.png`.

## (c) Notifications — **polling, ≈15-minute latency, not push**

Endpoints bound exactly as in `android/reference/api-routes/account.ts:44-63`:

| Call | Contract |
| --- | --- |
| `GET /account/notifications` (`:45-51`) | `{ unread, before, items[] }`, items `{ id, type, title, body, link, readAt, createdAt }`, 30 per page, cursor `before`, `unread=1` filter |
| `GET /account/notifications/unread` (`:53-56`) | `{ unread }` |
| `POST /account/notifications/read` (`:57-63`) | `{ ids?: uuid[1..100] }` or `{ all: true }` → `{ updated, unread }` (422 when neither is given) |

* Channel **"Bookings & account"**, `IMPORTANCE_DEFAULT`.
* Android 13+: `POST_NOTIFICATIONS` requested **once after the first successful sign-in** behind a one-line rationale dialog; the app never blocks when denied, and the ask is remembered in `NotificationPrefs`.
* `PeriodicWorkRequest` every 15 min with a `NetworkType.CONNECTED` constraint, scheduled only while signed in and cancelled on sign-out; one system notification per **new** unread item, using the API's own title/body (no invented copy), grouped under the channel. Last-seen id is persisted so nothing is shown twice.
* Tap → `servix://booking/<id>` (when `link` matches `/bookings/<id>`) opens Booking detail, otherwise `servix://notifications` opens the Notifications screen.
* Account › **Notifications** screen: list with unread dots, "Mark all read", manual refresh, empty and error states, and marking a row read when it is opened.

Deferred / not yet wired (call out for pass 4): the Home bell + unread dot (Home has no notification entry point yet), pull-to-refresh gesture (a refresh action in the top bar stands in), and tapping a notification does not mark it read until the row is opened in the app.

## Verification

* `Build test APK` — green (release `v0.6.3-b32`).
* `Screenshot renders` — green; 18 captures under `docs/screenshots/`.
* Device verification (Custom Tab return, notification permission dialog, worker timing) stays with the owner — none of it can run in Robolectric NATIVE.

## Consulted skills

`emil-design-eng` (spacing/hierarchy, hairline borders, no elevation), `mobile-native` and `design-mobile-apps` (thumb-reachable sticky bars, 48/52 dp targets, chip rows, Android 13 permission etiquette, Custom Tab over an embedded WebView), `jetpack-compose-skills` (`HorizontalPager`, `stickyHeader`, `LazyColumn` + overlay bar, `PeriodicWorkRequest`), `animate` and `review-animations` (150–250 ms ease-out, page-dot fade), `android-testing-skills` (Robolectric NATIVE capture harness, `testTag` coverage), `impeccable`, `ui-ux-pro-max`, `frontend-design` (type scale, price tabular figures, empty-state copy).
