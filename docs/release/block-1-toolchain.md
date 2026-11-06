# Release prep — Block 1: Play toolchain

Nothing has been uploaded or published. Owner action only: nothing in this block.

## Versions before → after

| Piece | Before | After |
| --- | --- | --- |
| Android Gradle Plugin | 8.7.3 | **8.13.0** |
| Gradle (both workflows, `gradle-version`) | 8.9 | **8.14.3** |
| Kotlin (android / compose / serialization plugins) | 2.0.21 | **2.4.10** |
| Compose BOM (app + tests) | 2024.12.01 | **2026.05.01** |
| `material-icons-extended` | via BOM | pinned **1.7.8** (no longer in the BOM) |
| compileSdk | 35 | **36** |
| targetSdk | 35 | **36** |
| minSdk | 26 | 26 (unchanged) |
| build-tools / platform (`setup-android`, apksigner path) | 35.0.0 / android-35 | **36.0.0 / android-36** |
| JDK (CI) | 17 | 17 (unchanged) |

## Why 2026.05.01 and not the newest BOM

Compose BOM **2026.09.00** (the newest stable) needs `compileSdk 37`: with `compileSdk 36` the build fails `checkDebugAarMetadata` with 22 AAR-metadata errors, because those artifacts are compiled against SDK 37. Since the Play requirement is *targetSdk* 36 and this block asked for compileSdk 36, the BOM is pinned to the newest release that supports compileSdk 36 (2026.05.01). Bumping to 2026.09.00 is a one-line change if we later accept `compileSdk 37` with `targetSdk 36`.

Versions were cross-checked against public repos because Google Maven is not reachable from the sandbox: AGP 8.13.0 + Gradle 8.14.3 (`netbirdio/android-client`), `compileSdk 36` + BOM 2026.05.01 (`sumup/sumup-android-sdk`).

## Behaviour changes handled

* **Edge-to-edge enforced (SDK 36).** The theme never set opaque system bars, `MainActivity` already calls `enableEdgeToEdge()`, and the shell keeps `contentWindowInsets = WindowInsets.safeDrawing`. All 27 renders were regenerated on the new toolchain and inspected: Home/Explore/AI/Account clear the status bar, the booking-detail action bar sits above the gesture inset, and full-bleed image pagers still draw under the bars.
* **Predictive back.** `android:enableOnBackInvokedCallback="true"` plus the existing `BackHandler` / `onBackPressedDispatcher` usage and `ModalBottomSheet` dismissal paths mean sheets close before the screen pops, with system back animations.
* **Notifications/permissions.** Unchanged: `POST_NOTIFICATIONS` is still requested once after the first successful sign-in (API 33+), no new runtime permissions, no foreground service.
* **`kotlinOptions` removed** in favour of `kotlin { compilerOptions { jvmTarget = JVM_17 } }` (deprecated in KGP 2.x, gone in the 8.13/2.4 pairing).

## Verification

* `Build test APK` green on `39b763c` → `v0.7.0-b43`.
* `Screenshot renders` green (Robolectric captures **and** `TouchTargetTest`), renders committed at 50 % for review (`aff9b30`), then the auto-commit sentinel was removed again.
* No app source changes were needed for the SDK bump; only build files, workflows and the manifest flag.

## Next block

Block 2 (release build type: minify/shrink + keep rules, self-update off, backup/cleartext off, 1.0.0/100) waits for "go".
