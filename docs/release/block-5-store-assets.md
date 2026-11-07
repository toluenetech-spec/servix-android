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
| `docs/play/feature-graphic-1024x500.png` | 1024×500 | 27,978 | Deep Forest canvas, the real brand wordmark (`servix-logo-dark.svg`: SX monogram + custom "Servix" lettering), tagline "Hire trusted Nigerian professionals.", coral rule |
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

Everything here is the owner's real artwork. There is nothing hand-drawn left in `docs/play/`.

* The **wordmark** in the feature graphic is `public/brand/servix-logo-dark.svg` from
  `toluenetech-spec/servix` (ref `arena/01a0dc4c-servix`), rendered as **vectors**, not typeset. The
  test reads the SVG, parses every `<path>` with `androidx.core.graphics.PathParser`, scales the whole
  lockup and draws it in Warm Ivory. It is the same file the website serves, byte-identical to
  `reference/web/public/brand/servix-logo-dark.svg` (sha256 `6609217108d675ea…`, 3,779 B).
* The **SX mark** in `icon-512.png` is the app's own launcher vector
  (`res/drawable/ic_launcher_foreground.xml`), and the test now proves it: it extracts the vector's
  `android:pathData` and asserts it equals path 0 of the brand SVG (whitespace-normalised). If anyone
  ever replaces the launcher mark with a lookalike, `playIcon512` fails.
* The **icon** is painted from exactly the two layers the adaptive icon declares — `@color/deep_forest`
  (#12372A) and that foreground vector. `AdaptiveIconDrawable` is deliberately *not* used: its
  mask/inset maths is the least reliable part outside a real device, and Play wants a flat opaque
  square anyway (the launcher applies the mask).
* The **palette** is `ui/theme/Color.kt`: Deep Forest `#12372A`, Warm Ivory `#F7F4EC`, Coral `#E56B5D`.
  The coral rule under the tagline is the only coral in the graphic.
* **Geometry, checked numerically before the render:** the SVG's `viewBox` is `0 0 403.4 76.7` but its
  path bounds exceed it, and `servix-logo-dark.svg` nests the lettering group
  (`translate(143.1 -13.8)`) *inside* the monogram group (`translate(2 73.7)`). A flat reading of the
  transforms puts the wordmark off-canvas, so `brandPaths()` walks `<g>`/`</g>`/`<path>` in document
  order with a stack and composes nested translates: `(2.0, 73.7)` for the monogram and `(145.1, 59.9)`
  for the lettering. At `markWidth = 620` the scale is 1.5369, the artwork is 117.9 px tall, and the
  resolved bounds are monogram x[210.5, 381.3] y[135.3, 250.1], wordmark x[429.0, 816.6]
  y[157.1, 228.4] — inside the 1024×500 canvas, centred to 1.6 px, with the tagline baseline at 336.7
  and the coral rule at 358.7–364.7. `check(paths.size == 2)` would fail if a reader flattened the
  groups into three.
* The **tagline** ("Hire trusted Nigerian professionals.") is still Inter 500, the app's own
  `res/font/inter_500.ttf`. That is a sentence, not brand lettering.

## CI history

| Run | Result | Why |
| --- | --- | --- |
| 42224677140 | failed | Two `drawText` calls passed `Int` where `Float` was required — compile error, so both jobs failed |
| 42225005610 | failed | Window assertion caught the `xxxhdpi` mistake (1440×2560, not 1080×1920) |
| 42225414418 | failed | Only `playIcon512`: `AdaptiveIconDrawable` did not paint reliably under Robolectric |
| **42225827800** | **green** | First good set; committed `docs/play/` |
| **42226236157** | **green** | Polish pass: home shot shows live bookings, mark enlarged for optical balance; re-committed |
| 42280296822, 42280853421 | failed | The brand-aware rewrite did not compile. Ten annotations said only *"Unresolved reference 'capturePlayPhone'"* — a cascade, never the root cause |
| 42281315965 | failed | Raised the diagnostics to 30 annotations across three steps; that finally showed the real error: `Syntax error: Missing '}'` at the end of `playFeatureGraphic1024x500` |
| 42355041266 | failed | Root cause: a KDoc line containing `public/brand/*.svg` — the `*/` **closed the comment early**, so prose was parsed as code and one brace went missing. Rewritten, and the whole asset section was rebuilt on the last green file rather than patched again |
| **42355410630** | **green** | Real brand artwork rendered, both jobs green; committed `3cec86d` and re-created the `ASSETS_COMMITTED` sentinel |

Two CI lessons worth keeping:

* **GitHub caps workflow-command annotations at ten per step.** A long Kotlin cascade therefore hides
  the one error that matters; the workflow now reports errors 1–10, 11–20 and 21–30 in three separate
  steps. That is what turned "unresolved reference" noise into the actual syntax error.
* **A `*/` inside a KDoc is code, not prose.** Any comment that quotes a glob like `public/brand/*.svg`
  silently truncates itself. Look for the earliest *syntax* error, not the first unresolved reference.

Verification was repeated locally on the committed files: all eight phone PNGs read `1080x1920`, the
icon `512x512`, the feature graphic `1024x500`, straight from the IHDR chunk. The committed bytes are
what CI rendered in run 42355410630 (`docs/play/ASSETS_COMMITTED` records the run and commit).

## Not in this block

No Play Console upload, no API call, no listing copy (that is Block 6's `docs/play/LISTING.md`) and no
release checklist (Block 7). The screenshots show only fixtures — the AI disclaimer, escrow wording and
status labels are the real in-app copy, unchanged.
