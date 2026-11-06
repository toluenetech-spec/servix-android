# Release prep — Block 5: Play store assets

All eleven assets are in `docs/play/`, rendered by CI from the existing Robolectric screenshot
harness. **Nothing was uploaded to Play** — these are files the owner drags into the listing.

## What was produced

| File | Size | Bytes | What it shows |
| --- | --- | --- | --- |
| `docs/play/phone/01_home.png` | 1080×1920 | 168,842 | Home signed in: "Needs your attention" (pay / confirm / review) plus the bookings list with an *Awaiting payment* card |
| `docs/play/phone/02_explore.png` | 1080×1920 | 194,529 | Explore in Professionals mode, query "logo designer", Filters·2, "12 professionals found", rated cards with ₦ start prices |
| `docs/play/phone/03_professional.png` | 1080×1920 | 171,641 | Tunde Bakare's profile: services, reviews, availability summary |
| `docs/play/phone/04_service.png` | 1080×1920 | 111,996 | Brand Identity Package detail with availability slot selected and Book & pay |
| `docs/play/phone/05_booking.png` | 1080×1920 | 158,475 | Booking detail in the `requested` state — the status timeline, not a payment screen |
| `docs/play/phone/06_ai.png` | 1080×1920 | 181,030 | Servix AI conversation with usage meter and the "drafts — check them" disclaimer |
| `docs/play/phone/07_notifications.png` | 1080×1920 | 95,895 | Notifications: booking accepted, ₦350,000 held in escrow |
| `docs/play/phone/08_account.png` | 1080×1920 | 149,865 | Account hub: profile, saved, notifications, plan, security, this device |
| `docs/play/icon-512.png` | 512×512 | 13,753 | Full-bleed Deep Forest square with the SX mark — opaque, no rounded corners |
| `docs/play/feature-graphic-1024x500.png` | 1024×500 | 27,978 | Deep Forest canvas, SX mark, "Servix", tagline "Hire trusted Nigerian professionals.", coral rule |
| `docs/play/ASSETS_COMMITTED` | — | 286 | Sentinel: keeps the art out of every subsequent run |

Total repository cost: **1.3 MB** for all eleven files.

## How they are generated

`app/src/test/java/ng/name/servix/app/screenshots/PlayAssetsTest.kt` — same harness as the review
renders (Robolectric `GraphicsMode.NATIVE`, offline fixtures, Retrofit pointed at `127.0.0.1:9`), with
two deliberate differences:

1. **No 50% downscale.** The review-render harness halves each bitmap to keep the repo small; Play
   assets are written at full pixel size and recycled afterwards.
2. **A different window qualifier.** `w360dp-h640dp-xxhdpi`. The task named `xxxhdpi`, but density
   matters: xxxhdpi is density 4, so a 360×640 dp window renders 1440×2560. Density 3 (xxhdpi) is
   what lands on **exactly 1080×1920**. The test asserts the window is 1080×1920 before it draws, so a
   wrong qualifier fails the job instead of silently shipping the wrong size (this is exactly how the
   mistake was caught: the first three CI runs failed on that assertion).

The job (`.github/workflows/screenshots.yml`, job `play_assets`) also verifies every PNG's width and
height by reading the IHDR bytes with `od` — no ImageMagick, no Python — and fails on a mismatch, then
commits `docs/play/` **once** behind the `ASSETS_COMMITTED` sentinel.

### Play's rules, and where each is satisfied

| Play requirement | Here |
| --- | --- |
| Phone screenshots: PNG/JPEG, 320–3840 px, aspect ratio no more than 2:1 | 1080×1920 = 1:1.78 |
| 8 phone screenshots | 8, `01_home` … `08_account` |
| App icon: 512×512, 32-bit PNG, no transparency | 512×512, `Color.alpha` asserted `255` at four corners and centre |
| Feature graphic: 1024×500, PNG/JPEG | 1024×500, background asserted opaque |

## Brand assets — what is real and what is not

* The **SX mark** is the app's own launcher vector (`res/drawable/ic_launcher_foreground.xml`), the
  same path data as `docs/servix-app-logo.png`. Not redrawn.
* The **icon** is painted from exactly the two layers the adaptive icon declares —
  `@color/deep_forest` (#12372A) and that foreground vector. `AdaptiveIconDrawable` is deliberately
  *not* used: its mask/inset maths is the least reliable part outside a real device, and Play wants a
  flat opaque square anyway (the launcher applies the mask). The first attempt used it and failed CI.
* The **palette** is `ui/theme/Color.kt`: Deep Forest `#12372A`, Warm Ivory `#F7F4EC`, Coral `#E56B5D`.
* **Flagged, not silent:** the *wordmark* in the feature graphic is typeset in **Inter 700** — the
  app's own bundled typeface (`res/font/inter_700.ttf`) — not the custom lettering in
  `reference/web/public/brand/servix-logo-dark.svg`. There is no SVG rasteriser in the unit-test JVM,
  so the test cannot draw that file. It is honest typography in the app's real font, not a
  hand-drawn imitation, but it is not the marketing wordmark. Tracked as follow-up #7.

## CI history

| Run | Result | Why |
| --- | --- | --- |
| 42224677140 | failed | Two `drawText` calls passed `Int` where `Float` was required — compile error, so both jobs failed |
| 42225005610 | failed | Window assertion caught the `xxxhdpi` mistake (1440×2560, not 1080×1920) |
| 42225414418 | failed | Only `playIcon512`: `AdaptiveIconDrawable` did not paint reliably under Robolectric |
| **42225827800** | **green** | First good set; committed `docs/play/` |
| **42226236157** | **green** | Polish pass: home shot shows live bookings, mark enlarged for optical balance against the wordmark; re-committed |

The renders job and the Play-assets job share the test source set, so a compile error breaks both —
that is how the first failure surfaced. The job now also converts JUnit `<failure message=…>` into
check-run annotations, because run logs are served from storage that is not always reachable.

Verification was repeated locally on the committed files: all eight phone PNGs read `1080x1920`, the
icon `512x512`, the feature graphic `1024x500`, straight from the IHDR chunk.

## Not in this block

No Play Console upload, no API call, no listing copy (that is Block 6's `docs/play/LISTING.md`) and no
release checklist (Block 7). The screenshots show only fixtures — the AI disclaimer, escrow wording and
status labels are the real in-app copy, unchanged.
