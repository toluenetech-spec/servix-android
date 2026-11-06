# Pass 5 — motion, haptics, accessibility, polish

**0.7.0** / versionCode **14** · release `v0.7.0-b40` · cert `cf6460a5…7299d` · both workflows green.

## 1. Motion (every animation + duration)

| Motion | Duration / easing | Where |
| --- | --- | --- |
| Screen push | shared-axis X: 220 ms, new screen slides 25 % from end + fade, old drifts 8 % | `NavHost` in `ServixAppShell` |
| Screen pop | 220 ms, mirrored | same |
| Tab switch | crossfade, 120 ms | `Home`, `Search`, `Bookings`, `ServixAi`, `Account` composables |
| Bottom sheets | 250 ms (Material sheet default, `Motion.SHEET_MS` documents the budget) | `ServixBottomSheet`, booking sheet, More sheet |
| Skeleton → data | crossfade, 150 ms budget (`Motion.CROSSFADE_MS`) | list loading branches |
| Button/row press | scale 0.98 + 8 % darker, 120 ms, `FastOutSlowInEasing` — no overshoot, no bounce | `Modifier.pressFeedback` on primary/secondary buttons |
| Streaming caret | blink 600 ms, `RepeatMode.Reverse`, linear | AI chat |
| Easing everywhere | `FastOutSlowInEasing` (tween) | all of the above |

`Motion.duration()` reads `Settings.Global.ANIMATOR_DURATION_SCALE`; at `0` every duration becomes **0 ms** (no fade, no slide, caret static), so "Remove animations" is honoured.

## 2. Haptics

One light tick only — `HapticFeedbackConstants.CONFIRM` plus `HapticFeedbackType.LongPress` — on: Book & Pay success, Confirm completion, Review submitted (all three arrive as `actionSuccessMessage` in booking detail) and Sign-in success (shell, on a false → true auth transition). Nowhere else.

## 3. Accessibility

* **Contrast:** `MutedGray` was `#68716B` → 4.13:1 on Soft Sand (failing). Darkened to **`#5F675F`**: 4.78:1 on Soft Sand, 5.32:1 on Warm Ivory. Body/Charcoal `#202622` and Deep Forest pass comfortably.
* **Touch targets:** `ServixSegmentedControl` raised from 44 → 48 dp, AI icon pills 32 → 48 dp. New `TouchTargetTest` walks the semantics tree of nine screens (Welcome, Sign in, Explore, Bookings, Booking detail, Account, Notifications, AI) and fails if any clickable node is under 48 dp **or** has no text/contentDescription. It is part of CI.
* **Headings:** `Modifier.heading()` on Books/AI/Account/Booking-detail/Notifications titles so TalkBack can jump sections.
* **1.3× text:** `03c_home_fontscale130`, `06b_explore_fontscale130`, `11b_booking_detail_fontscale130`, `14c_ai_fontscale130` render at `fontScale = 1.3`.
* **Status chips** expose their label as text (`StatusChip` renders the words "Awaiting payment", "In progress"…), never colour alone.

## 4. Polish

Sentence-case strings, no exclamation marks; IME actions set per field (Next/Done/Search/Send); sheets are `ModalBottomSheet`s so back closes the sheet before popping; `ErrorStateCard` renders `ServixApiException.message` only (no JSON/stack text); offline banner sits in the shell top bar on every tab; Saved and Notifications both have empty states.

## 5. Profile photo — implemented

`POST /uploads?kind=avatar&fileName=…` accepts the raw body with its own content type, `requireAuth`, 40 requests / 10 min (`api/src/routes/uploads.ts:67-91`), returns `{ url, key, kind, size }`; avatars share the `ALLOWED_IMAGE_TYPES` rule capped at `MAX_UPLOAD_BYTES` = 5 MB (`api/src/lib/storage.ts:41-51`). `PATCH /account/profile { avatarUrl }` validates it with `assertOwnMediaUrl` and also syncs the professional card (`account.ts:14-31`).
Picker: `PickVisualMedia(ImageOnly)` in Profile → read bytes → reject > 5 MB or non-image → upload → PATCH avatarUrl → refresh avatar.

## 6. Render weight

Harness now writes **50 %-scale** PNGs (616 × 1371 px instead of 1232 × 2742), and the workflow uploads a `screen-renders` artifact on every push while committing into `docs/screenshots/` only when the `docs/screenshots/COMMIT_RENDERS` sentinel exists — it was present for this pass and removed again, so future pushes stay artifact-only. Repo cost this pass: 27 files, 2.9 MB.

## Screenshots

All 23 existing captures re-rendered at 50 % with the new `MutedGray`, 48 dp targets and press-state styling; four new captures for 1.3× text (`03c`, `06b`, `11b`, `14c`).

## Honest gaps

Crossfade for skeleton → data is documented and applied on the loading branches of the list screens but not every sub-panel. Sheet durations come from Material defaults (250 ms budget, not overridable). The a11y test checks size and labels, not contrast or reading order automatically. Device verification of haptics and the photo picker stays with the owner.

## Skills consulted

`emil-design-eng` (no bounce, fast press response, 150-250 ms budgets), `animate` / `review-animations` (shared-axis X, caret blink, easing choice), `mobile-native` (haptics discipline, picker over permissions), `android-testing-skills` (semantics-tree assertions in Robolectric), `jetpack-compose-skills` (`graphicsLayer`, `infiniteRepeatable`, `NavHost` transition lambdas), `impeccable` / `ui-ux-pro-max` / `frontend-design` (contrast maths, type scaling).
