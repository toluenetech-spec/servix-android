# Pass 4 — Bookings, Booking detail, Servix AI, Account, Auth

Version **0.6.4** / versionCode **13** · branch `arena/f657a17e-servix-android`
Release `v0.6.4-b35` (APK) · cert SHA-256 `cf6460a5e679cd797779551af48b1bf9dd8dda8f524a14db55489ae49717299d`
CI: `Build test APK` green, `Screenshot renders` green (19 captures).

## Fixes carried in first

* **Avatar initials** now come from `ServixFormatters.extractInitials` = first letter of the first name + first letter of the **last** name, one letter when there is a single name (`AvatarImage` normalises a full name through `avatarLabel`, so "Tunde Bakare" → `TB`, "Adaeze Okonkwo" → `AO`). Account header, Home avatar, rows and review pills all use it.
* **Chip rows never hard-clip**: Home categories, Home upcoming-bookings and the Explore filter row are full-bleed rows with `contentPadding = start 16 dp / end 8 dp`, an 8 dp trailing spacer and 8 dp item spacing, so the next chip always peeks. Category/booking rows also got 16 dp gutter parity.

## Home bell

`HomeTopBar` gained a bell (`home_notifications_button`) with an 8 dp `ForestGreen` dot when `unreadNotifications > 0`; the shell polls `GET /account/notifications/unread` (`account.ts:53-56`) on sign-in state and destination changes and drops the count on sign-out. Tapping opens the Notifications route.

## Bookings

`BookingsScreen` uses `ServixSegmentedControl` (Active/Past), a `supportsPullToRefresh` `PullToRefreshBox` (no top-bar refresh), and groups rows **Today / This week / Later** off the Lagos-local date. New compact `BookingRow` (status chip + price, service, professional · slot, chevron) replaces the tall cards. Notifications also switched to `PullToRefreshBox`.

## Booking detail

Status header card (`booking_status_header`) carries the chip, the amount and **one plain sentence** of what happens next, per state (`bookingNextStepSentence`). Actions moved out of the card into a sticky `BookingActionBar`: exactly one primary next step (Pay / Confirm work is done / Leave a review / Book again) plus a **More** sheet for the rest (Cancel, Report a problem). The Stage 3 gates, payment polling, banners, refresh and success/error handling are untouched.

## Servix AI

Real chat layout: bubbles (user right, ForestGreen; assistant left, white hairline card), streaming caret (`▍` in `ai_streaming_bubble`), suggestion chips when the thread is empty, composer pinned above the nav bar with `imePadding()`, thin usage bar + `2,300 / 20,000` label in the top bar, rounded icon pills (copy / helpful / not helpful) under each answer, Assistant · Budget guide segmented control.

## Account

`AccountHubScreen`: 64 dp avatar header (name, email, role chip, verify-email CTA) and grouped rows — Profile, Saved, Notifications (unread dot), Plan, Security, This device, Updates, Legal — then Sign out and a website-only note for deletion.
Bindings: **Profile** `PATCH /account/profile { fullName }` (2-200 chars, inline validation, disabled until changed); **Saved** `GET /account/saved` + `DELETE /account/saved/{slug}`; **Plan** `GET /billing/plan` (alias of `/me/entitlements`, read-only, "Manage on the website"); **Security** `GET /account/security-summary` (only the fields it returns) + "Manage on the website"; **This device** keeps `DeviceCard`; **Updates** keeps `UpdateCard`; **Sign out** posts `/auth/logout`, clears tokens/DataStore except Terms, and the shell cancels the notification worker when the session drops.

## Auth

`ServixTextField` now uses a **floating label inside a 56 dp field** (required marker inside the label), inline error text under the field, password visibility toggle unchanged; `AuthShellContainer` pads for the IME so the submit button stays above the keyboard. Security check opens with a **4-dot progress row** (`security_step_progress`) plus the step name (`security_step_name`), derived from the live flow state (Email → Authenticator → Recovery codes → Done).

## Screenshots (per file)

| File | What changed |
| --- | --- |
| `10_bookings_grouped.png` | Segmented Active/Past, Today / This week / Later headers, compact rows |
| `11_booking_pending_payment.png` | Status card + "Pay now to hold this slot…", bottom bar `More` + `Pay ₦350,000` |
| `12_booking_requested.png` | "Waiting for the professional to accept…", bar with More + next step |
| `13_booking_completed.png` | "All done and paid. Leave a review…", review CTA in the bar |
| `14_ai_chat.png` | Two exchanges, bubbles, icon pills, pinned composer, usage bar |
| `14b_ai_empty.png` | Empty thread with intro bubble + suggestion chips |
| `15_account.png` | 64 dp avatar header (`AO`), grouped settings, unread dot, sign out |
| `16_notifications.png` | List with unread dots, mark-all-read, pull-to-refresh |
| `17_plan.png` | Read-only plan card with "Manage on the website" |
| `18_security_check_step.png` | Step 2 of 4, four-dot progress row, step name |

## Honest gaps

Profile photo upload is not wired (`avatarUrl` accepted by the API but no picker). Saved/Plan/Security panels have no pull-to-refresh. The bell only refreshes on navigation, not on a timer. Notification taps still rely on the Pass 3 deep links. Device verification (IME behaviour, pull gestures, real API shapes for `/billing/plan` and `/account/security-summary`) stays with the owner.

## Skills consulted

`emil-design-eng` (row rhythm, 56 dp list rows, hairline dividers), `mobile-native` / `design-mobile-apps` (sticky action bars, segmented controls, IME-aware composer, chat conventions), `jetpack-compose-skills` (`PullToRefreshBox`, `LazyColumn` grouping keys, `imePadding`, `stickyHeader`-free pinned footers), `animate` / `review-animations` (150-250 ms ease-out, streaming caret), `android-testing-skills` (Robolectric captures, `testTag` coverage for every new state), `impeccable`, `ui-ux-pro-max`, `frontend-design` (type scale, tabular prices, calm empty states).
