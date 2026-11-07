# Servix — Play Console release checklist (Block 7)

The exact tap-path from "no app in Play" to a live internal-testing build, written for a phone
(Chrome on Android, Play Console website). Nothing here publishes to production: internal testing is
the last step, and you stop there.

Everything the console asks you to type is in **`docs/play/LISTING.md`** — keep it open in another tab.

---

## 0. Facts you will be asked to confirm

| Item | Value | Where it comes from |
| --- | --- | --- |
| Package name | `ng.name.servix.app` | `app/build.gradle.kts:14` — **permanent, cannot be changed after the first upload** |
| App name | Servix | `docs/play/LISTING.md` §1 |
| Version | versionName `1.0.0`, versionCode `100` | `app/build.gradle.kts:17-18` |
| minSdk / targetSdk / compileSdk | 26 / **36** / **36** | `app/build.gradle.kts:10,15-16` |
| Upload-key certificate SHA-256 | `BC:31:4D:57:84:2B:A6:BF:FE:D5:77:11:EB:13:78:FA:1C:BC:6B:0A:52:A9:1F:1A:F6:82:16:12:15:B0:2D:94` | `docs/release/block-3-upload-key.md:16` |
| Bundle to upload | `servix-1.0.0-play-build5.aab` | workflow run **#5** of "Build Play release bundle" |
| Mapping (keep, do not upload) | `servix-1.0.0-mapping-play-build5.txt` | same run |
| Privacy policy | `https://www.servix.name.ng/privacy` | in-app links at `AuthScreens.kt:681`, `AccountHubScreen.kt:555` |
| Terms | `https://www.servix.name.ng/terms` | `AuthScreens.kt:667`, `AccountHubScreen.kt:561` |
| Contact email | `toluenetech@gmail.com` | `reference/web/src/content/legal.js:3` |

**Before you start, check the download is the right one.** Open GitHub → **Releases** → the entry
headed **"Play upload bundle v1.0.0 (play-v1.0.0-b5)"**. Its table must show:

* `File` = `servix-1.0.0-play-build5.aab`
* `Bundle versionName / versionCode` = `1.0.0 / 100`
* `Upload-key certificate SHA-256` = the fingerprint in the table above
* `Signature` = `AGP, as built` or `re-signed by this job with jarsigner (servix-upload)`

If the file you get is named `…-play-build4.aab`, you grabbed run #4 — go back one release. If the
workflow run itself is red, **stop**: a red run's bundle must not be uploaded.

Download these too (tap each, then "Download"): the icon, the feature graphic and the eight phone
shots. On the branch they live at
`https://raw.githubusercontent.com/toluenetech-spec/servix-android/arena/e9ca0c44-servix-android/docs/play/…`
(swap `arena/e9ca0c44-servix-android` for `main` after PR #2 is merged):

| File | Play field | Size |
| --- | --- | --- |
| `icon-512.png` | App icon | 512×512 |
| `feature-graphic-1024x500.png` | Feature graphic | 1024×500 |
| `phone/01_home.png` … `phone/08_account.png` | Phone screenshots (in that order) | 1080×1920 each |

Everything lands in your phone's **Downloads** — that folder is what the file pickers will offer.

---

## 1. Create the app

1. Go to `https://play.google.com/console` in Chrome, sign in with the account that owns the developer
   profile.
2. **All apps → Create app**.
3. Fill in:
   * **App name**: `Servix`
   * **Default language**: `English (United States)` — or `English (Nigeria)` if it appears in the
     list; the app is en-NG. This only sets the first listing language, it is not a restriction.
   * **App or game**: `App`
   * **Free or paid**: `Free`
4. Tick **both** declaration checkboxes (Play Developer Program Policies; US export laws).
5. **Create app**.

> You cannot change the app name or the free/paid status (free → paid is impossible) after this, so
> check the spelling before you tap.

## 2. Dashboard → "Set up your app"

Play lists these as tasks down the Dashboard. Do them in this order; several of them **block the
rollout** until they are answered.

### 2.1 App access (the reviewers' way in)

**Dashboard → App access → Manage → Add new instructions.**

* **Name**: `Servix test account`
* **Username**: the email of the test account (fill in the row below)
* **Password**: type the real password here, **in the console only**
* **Instructions** (paste this):

```text
Servix is a services marketplace. Browsing the Explore tab, professional profiles and service pages
works without signing in. Signing in is needed for bookings, reviews, notifications and Servix AI.
Use the username and password above. The account is a normal customer account with a verified email.

Where to look:
• Explore tab: search, category filters, professional and service cards.
• A professional profile: services, portfolio, ratings and reviews (posted by other customers).
• A service page: availability slot picker and the "Book and pay" path. Payment opens Paystack's
  hosted checkout in Chrome; nothing is charged without you completing that page.
• Bookings tab: booking status timeline, "Report a problem" (dispute) and "Confirm completion".
• Account tab: profile, saved professionals, notifications, plan; plan, security and settings open
  the website in a browser tab.

Notes: notifications are delivered by a periodic check (they can lag about 15 minutes), there is no
in-app chat or community feed, and there are no ads.
```

**The test account itself — you have to create this, once, on production:**

1. In another tab open `https://www.servix.name.ng/register`.
2. Register a fresh customer account with an email you control (any address works; do not reuse your
   own admin/owner login).
3. Verify the email if the app asks for it — an unverified account will fail the reviewer's sign-in.
4. **Write the email in the table below and nothing else.**

| Test account email | ⟨fill in — the address you registered⟩ |
| --- | --- |

> **Never write the password in this file, in a commit, in chat or in a ticket.** It exists in exactly
> two places: your own password manager and Play Console → App access. If the account is ever
> compromised, change its password in the console and re-save the App access form.

### 2.2 Ads

**App content → Ads → No, my app does not contain ads.**
(No ad SDK is in `app/build.gradle.kts:100-121`; there are no ad units in any screen — LISTING.md §4.)

### 2.3 Content rating questionnaire

**App content → Content rating → Start questionnaire.**

* **Email address**: `toluenetech@gmail.com`
* **Category**: choose `Utility, Productivity, Communication, or Other` (not a game).
* Answer every content question **No** — violence, blood, sexuality, nudity, profanity, drugs,
  alcohol, tobacco, gambling (real and simulated), horror, discrimination. The full key is LISTING.md
  §7.
* Then the "interactive elements" questions:

| Question | Answer | Why |
| --- | --- | --- |
| Do users interact or exchange content? | **Yes** | Customers post a star rating + text review after a booking (`BookingModels.kt:50`, `BookingRepository.kt:107-117`), and those reviews are visible to other users (`ServixApiService.kt:209,239`) |
| Can users communicate directly with each other? | **No** | No chat, DM or community feed exists in the app |
| Can users share their current location with others? | **No** | The approximate city stays on the device (`LocationButton.kt:42`) |
| Does the app allow purchases? | **Yes** | Real-world services, paid in Paystack's hosted checkout (`BookingDetailScreen.kt:1047`) |
| Does the app contain ads? | **No** | — |
| Digital goods / loot boxes / crypto | **No** | — |

Save, then **Submit**. Expect a low rating plus the "Users Interact" badge; no ads badge. Ratios and
badges are Google's to decide — accept whatever it returns.

### 2.4 Target audience and content

**App content → Target audience and content → Start.**

* **Age groups**: tick **18 and over** only. Leave every younger bracket unticked.
* **Is your app designed for families?** `No`.
* **Do you want your app to appeal to children?** `No`.
* **Store presence**: your listing does not target children (LISTING.md §8).

This matches the legal text: "You must be at least 18" (Terms) / "intended for people aged 18 or
over" (Privacy) in `reference/web/src/content/legal.js`. The app has no age gate; the declaration is
the control.

### 2.5 News app

**App content → News app → No.**

### 2.6 COVID-19 contact tracing

If the console still shows it: **No**.

### 2.7 Data safety

**App content → Data safety → Start.** This is the longest form; answer it from LISTING.md §4–§6,
section by section. The console's own path is:

**a. "Data collection and security"** → Next.
**b. "Does your app collect or share any of the required user data types?"** → **Yes**.
**c. Data types** — the console shows a checklist. Only these, exactly:

| Console section | Tick | Collected | Shared | Optional? | Purpose to pick |
| --- | --- | --- | --- | --- | --- |
| Personal info → **Name** | ✓ | Yes | No | Required | App functionality; Account management |
| Personal info → **Email address** | ✓ | Yes | No | Required | App functionality; Account management |
| Personal info → **User IDs** | ✓ | Yes | No | Required | App functionality; Account management |
| Financial info → **Purchase history** | ✓ | Yes | No | Required | App functionality |
| Photos and videos → **Photos** | ✓ | Yes | No | **Optional** | App functionality |
| Messages → **Other in-app messages** | ✓ | Yes | No | Optional | App functionality |
| App activity → **Other user-generated content** | ✓ | Yes | No | Optional | App functionality |
| Device or other IDs → **Device or other IDs** | ✓ | Yes | No | Required | App functionality; Fraud prevention, security and compliance |

Leave **everything else unticked** — in particular Location (both kinds), Phone number, Address,
Contacts, Calendar, Audio, Files and docs, Health, Web browsing, App interactions, Crash logs,
Diagnostics. Reasons, with code citations, are in LISTING.md §4. The three that people get wrong:

* **Location is not collected.** One user-initiated coarse fix becomes a city name in on-device
  storage and is never transmitted (`LocationButton.kt:38-42`, `AndroidManifest.xml:5`). Play's
  "collected" means "leaves the device", so the answer is no. Do not tick it "to be safe" — a
  declaration that contradicts the app is itself a review failure.
* **No phone number, no card data.** There is no phone field anywhere, and the app never renders a
  payment form: Paystack's hosted checkout opens in Chrome (`BookingDetailScreen.kt:1047`). Financial
  info stays unticked; only *purchase history* is ticked.
* **Device or other IDs are collected**, because every request carries
  `ServixAndroid/<version> (Android <release>; <manufacturer> <model>)` in the User-Agent
  (`ClientUserAgent.kt:8,12`). No advertising ID, serial or IMEI is used.

**d. Security practices:**

| Question | Answer |
| --- | --- |
| Is all of the user data encrypted in transit? | **Yes** — HTTPS-only, `network_security_config.xml:4-8`, `usesCleartextTraffic="false"` |
| Do you provide a way for users to request that their data is deleted? | **Yes** |
| Deletion request URL | `https://www.servix.name.ng/privacy` (the policy carries both the email route and the retention explanation) |
| Have you had an independent security review? | **No** — answer honestly; no third-party audit has been done |

**e. Review the summary**, tick the accuracy declaration, **Save**.

### 2.8 Government apps

**App content → Government apps → No** (if shown).

### 2.9 Financial features

**App content → Financial features** (the console may call it "Financial products and services").

* **Does your app provide financial products or services?** → **No.** Servix does not lend, hold
  funds, offer banking, crypto, insurance or investment products.
* **What the app does instead** (if there is a "describe the payments" or "additional information"
  box, paste this):

```text
Servix sells real-world professional services (design, photography, web development and similar).
When a booking needs payment, the API returns a Paystack authorization URL and the app opens it in the
user's browser via a Custom Tab. Card details are entered on Paystack's hosted page and are never
handled by the app. Play Billing is not used because the goods are real-world services, not digital
content.
```

* If the console asks whether the app uses Google Play's billing: **No** (real-world services).

### 2.10 Store settings, category and contact details

**Store presence → Main store listing → Contact details** and **Store settings**:

* **Category**: `Business` (app category) — you can add `Productivity` as the second tag.
* **Email**: `toluenetech@gmail.com`
* **Website**: `https://www.servix.name.ng`
* **Privacy policy**: `https://www.servix.name.ng/privacy` ← this one is checked by the reviewer, the
  page must open without a login
* **Phone**: leave blank (optional)

## 3. Store listing — the assets and the text

**Store presence → Main store listing.**

1. **Short description** — paste from LISTING.md §2 (63 characters).
2. **Full description** — paste from LISTING.md §3 (2,389 characters). Keep the blank lines; Play
   wraps the text itself.
3. **App icon** — upload `icon-512.png`.
4. **Feature graphic** — upload `feature-graphic-1024x500.png`.
5. **Phone screenshots** — upload all eight, in order: `01_home`, `02_explore`, `03_professional`,
   `04_service`, `05_booking`, `06_ai`, `07_notifications`, `08_account`. Play shows the first as the
   hero, so 01 must stay first.
6. Skip 7-inch/10-inch tablets, Chromebook and the promo video — none are mandatory.
7. **Save**. If Play complains about a missing required field, it names it in red under the field.

## 4. Testing → Internal testing → first release

1. **Testing → Internal testing → Create new release.**
2. **Play App Signing** box appears because this is the first bundle. Read it and choose
   **Use Play App Signing / Accept Google-managed signing** — Google holds the app signing key, you
   keep signing uploads with the `servix-upload` key (fingerprint in §0). Do not choose "export and
   re-upload my own key"; there is nothing to gain and Play cannot help you recover a lost key.
3. **App bundles → Upload**: pick `servix-1.0.0-play-build5.aab` from Downloads.
   Wait for the upload to finish — Play checks the signature and the version code and shows errors
   right there (see §7 if one appears).
4. **Release name**: `1.0.0 (100)`. (Play usually pre-fills `100 (1.0.0)`; either is only a label.)
5. **Release notes** — paste, or write your own:

```text
<en-GB>
First internal test build of Servix 1.0.0.

• Discover professionals and compare services
• Book a service and follow the booking status
• Pay for an accepted booking through Paystack's hosted checkout
• Notifications, reviews and the Servix AI assistant
```

6. **Next → Save → Review release.**
7. Read the warnings Play lists. Warnings are allowed; **errors** must be fixed. Then
   **Start rollout to Internal testing → Rollout**.
8. Leave **Managed publishing** off for internal testing; the release goes live immediately. (Managed
   publishing only matters for production, and it is on the production track.)

Play creates a **draft** until you press Start rollout — nothing reaches testers before that.

## 5. Testers tab — who can install it

1. **Testing → Internal testing → Testers** tab.
2. **Create email list** → name it `Servix internal testers`.
3. Add the Gmail addresses that should test (your own account can be one of them). Each tester must be
   signed into Play on an Android device with that Google account.
4. **Save changes**.
5. Copy the **opt-in link** (looks like `https://play.google.com/apps/internaltest/…`) and send it to
   the testers.
6. Tester installs it: open the opt-in link → **Become a tester** → **Download it on Google Play**.
   Install can take a few minutes to appear; if the link says "not available", the rollout is still
   processing — retry after 10–15 minutes.

## 6. What you have just done, and what you have not

* Done: the app exists in Play, is signed by Google's app signing key, and one 1.0.0 (100) bundle is
  live on the internal testing track with a tester list and an opt-in link.
* Not done: nothing is on production, nothing is public, and no store listing review has been
  requested. Closed/internal testing does **not** require the full review that production does, but
  the declarations above still gate the rollout.
* Keep the `.aab` and the mapping file: the mapping is what turns an obfuscated crash stack into
  readable class names. (The bundle already embeds a copy in `BUNDLE-METADATA/`, so Play's own crash
  reports deobfuscate without a manual upload.)

## 7. If Play shows an error, this is what it means

| Message (or close to it) | What it actually means | What to do |
| --- | --- | --- |
| *"You need to target API 36"* / *"Your app must target Android 15 (API 35) or higher"* | Play's annual target-API rule (API 35 from Aug 2025, rising to 36). **This cannot happen to this build**: `targetSdk = 36` and `compileSdk = 36` (`app/build.gradle.kts:10,16`), well above the floor, with `minSdk = 26` so older phones stay supported. | Nothing. If you see it, you uploaded an old `.aab` — check the version table in §0. |
| *"Your app bundle contains native code that is not 16 KB page-size compatible"* | Google's 16 KB memory-page requirement for Android 15+ devices. **This cannot happen here**: the project ships no native code at all (`find app/src -name "*.so"` and any `jniLibs` folder return nothing — it is pure Kotlin/JVM + AndroidX). | Nothing. |
| *"Version code 100 has already been used"* | A release with versionCode 100 already exists for `ng.name.servix.app` — either an earlier manual upload or a release you rolled out. | Bump to 101 (§8), rebuild, upload the new bundle. Do **not** try to re-upload 100. |
| *"Your app is signed with the wrong key"* / *"The certificate does not match the upload certificate"* | The `.aab` was not signed with `servix-upload`. The build falls back to the shared test key when the keystore secrets are missing (`app/build.gradle.kts:74-78`). | Compare the release page's *Upload-key certificate SHA-256* with §0. If it differs, the run did not have the keystore secrets — re-run the workflow and upload only the bundle whose fingerprint matches. |
| *"The app bundle is not signed"* / *"The bundle is corrupt"* | The upload was truncated (mobile downloads do this) or came from a red run. | Re-download from the release page, don't upload from a partial file. The workflow fails a build whose signature does not verify, so a green run cannot produce this. |
| *"App bundle was not zip-aligned"* / *"Unable to optimise your app"* | Transient Play processing, not your build. AGP always emits an aligned, optimised bundle. | Retry the upload once; if it repeats, re-run the workflow and upload the fresh bundle. |
| *"Your privacy policy URL is invalid / could not be reached"* | Reviewers must open it without a login, from outside Nigeria. | Open `https://www.servix.name.ng/privacy` in an incognito tab. Fix hosting or the URL before retrying — do not shorten it to the homepage. |
| *"App access: we could not sign in with the credentials you provided"* | The demo account's password was mistyped, the account was never email-verified, or it was deleted. | Sign in yourself with the same credentials on a device, then re-save the App access form. Verify the email first. |
| *"Your Data safety declaration is inconsistent with the app"* | An answer contradicts observable behaviour — the usual cause is ticking Location or Financial info, or unticking Device IDs. | Re-read LISTING.md §4 and correct the specific type. Do not add a type you do not collect "for safety". |
| *"You must complete the content rating questionnaire / target audience"* | A declaration is still open, so the rollout is blocked. | Dashboard → the red task → finish it. |
| *"Feature graphic is missing"* / *"Screenshots are required"* | The listing is incomplete. | Upload `feature-graphic-1024x500.png` and the eight phone shots (`docs/play/`). |
| *"No testers / this track has no testers"* | The tester list is empty. | §5 — create the list and add at least one email. |
| *"Your app has requested photo/video permissions without a declaration"* | Cannot happen: the app uses the system Photo Picker and declares no media permission (`AndroidManifest.xml:3-6`). | If it appears, you uploaded a different app — verify the package name on the upload page. |
| *"App contains ads"* declaration mismatch | Can happen only if you answered Yes to Ads. | It is **No** (§2.2). |

## 8. versionCode bump rule — for every bundle after this one

Play identifies a build by `versionCode` (100 in this release). Two rules, both hard:

1. **Every upload must use a versionCode that has never been uploaded for `ng.name.servix.app`.**
   Play rejects a repeat. `1.0.0 (100)` is now used; the next upload is 101.
2. **It only ever goes up.** Never lower it, never reuse a number from a previously rolled-out
   release. If you must replace a broken *draft* that was never rolled out, delete the draft release
   (not the app) and you may re-upload the same code — but once it has started rolling out, the number
   is spent.

To ship the next build:

1. Edit `app/build.gradle.kts:17-18`:
   ```kotlin
   versionCode = 101
   versionName = "1.0.1"   // optional; keep "1.0.0" if the change is invisible to users
   ```
2. Commit and push to the release branch.
3. Run **Actions → Build Play release bundle → Run workflow** (it is manual-only, on purpose).
4. Download `servix-1.0.1-play-build6.aab` (the number after `build` is the workflow's run number,
   not the versionCode — they are different counters and will drift apart).
5. In Play: **Internal testing → Create new release**, upload it, release name `1.0.1 (101)`, rollout.

Never skip a step by editing the versionCode in the console — it comes from the bundle, not from Play.

---

## 9. Where this stage ends

The release stage is complete when:

* the app exists in Play with package `ng.name.servix.app`,
* `1.0.0 (100)` is rolled out on **Internal testing** (not production),
* the tester list has at least one email and the opt-in link has been shared.

**STOP.** No production rollout, no closed/open testing, no store review request. When the internal
test comes back clean, the next step is a production track release — a separate decision, and a
separate checklist.
