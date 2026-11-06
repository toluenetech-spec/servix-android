# Release prep — Block 2: release build type

Version **1.0.0** (versionCode **100**) · release APK `servix-1.0.0-release-build48.apk` (3.46 MB) published as pre-release `v1.0.0-b48`. Nothing was uploaded to Play.

## Build type changes (`app/build.gradle.kts`)

| Setting | Value | Why |
| --- | --- | --- |
| `isMinifyEnabled` | **true** | R8 shrinking/obfuscation for the Play build |
| `isShrinkResources` | **true** | removes unused drawables/strings/layouts |
| `proguardFiles` | `proguard-android-optimize.txt` + `proguard-rules.pro` | AGP optimised defaults plus our keeps |
| `isDebuggable` | **false** | explicit, not inherited |
| `SELF_UPDATE_CHANNEL` | `"none"` | `Updates.check()` returns null unless the channel is `"github"` (`Updates.kt:82`) and `UpdateUiState.enabled` is `BuildConfig.SELF_UPDATE_CHANNEL == "github"` (`UpdateUi.kt:43`), so the updater is unreachable in release — asserted by `ReleaseSmokeTest` |
| versionName / versionCode | **1.0.0 / 100** | first Play version, versionCode leaves room for the next 900 uploads |
| signing | test key until Block 4 | keeps the minified APK installable over the current test builds |

Debug keeps `SELF_UPDATE_CHANNEL = "github"`, so the existing update path for testers still works.

## Manifest / network

* `android:allowBackup="false"` was already set — keeps access/refresh tokens out of cloud backup.
* `android:usesCleartextTraffic="false"` **added** and `android:networkSecurityConfig="@xml/network_security_config"` **added**.
* `res/xml/network_security_config.xml`: `cleartextTrafficPermitted="false"`, trust anchors limited to `system` (no user-installed CAs). The API base URL is HTTPS already (`https://api.servix.name.ng/api/v1/`), and the only browser hops are Chrome Custom Tabs, which are unaffected by the app's config.

## R8 keep rules (`app/proguard-rules.pro`, previously empty) and why

| Rule | Reason |
| --- | --- |
| `-keepclassmembers @Serializable class ** { Companion; INSTANCE; serializer(...) }` + `-keep,includedescriptorclasses ...data.model.**$$serializer` | kotlinx.serialization resolves serializers reflectively through `serializer<T>()`; R8 sees no call edge and strips the generated `Companion.serializer()` / `$$serializer` classes |
| `-keepclassmembers enum * { $VALUES; *; }` | enums are encoded by name |
| `-keepattributes *Annotation*, Signature, InnerClasses, RuntimeVisibleAnnotations` | the serialization plugin and Retrofit read annotations and generic signatures |
| `-keepclasseswithmembers interface * { @retrofit2.http.* <methods>; }` | Retrofit's dynamic proxy implementation looks up `@GET`/`@POST`/`@PATCH`/`@DELETE` annotations on `ServixApiService` |
| `-keep,allowobfuscation,allowshrinking interface ServixApiService` + `Continuation` | suspend functions on the interface are reflected over |
| `-keep class kotlin.Metadata` + `-dontwarn` for okhttp platform classes, conscrypt, bouncycastle, openjsse, slf4j, graalvm, kotlinx.serialization.internal | optional integrations we never ship; without the `-dontwarn` set R8 failed the release build |
| `-keep class NotificationsWorker` | WorkManager stores the class name in its database and re-instantiates it reflectively |
| `-keepnames ...data.model.**` + `SourceFile/LineNumberTable` | readable Play Console stack traces (obfuscated frames are de-obfuscated with `mapping.txt`) |
| `-dontwarn coil.**`, `-keep class coil.Coil` | Coil touches its own builder reflectively |

## Proof the minified build works (CI, run 42214912838)

1. `:app:assembleRelease` — succeeds with R8 + resource shrinking.
2. `:app:testReleaseUnitTest --tests 'ng.name.servix.app.release.*'` — **release-variant smoke tests** (`ReleaseSmokeTest`):
   * `BuildConfig` is `1.0.0 / 100`, channel is `none`;
   * `UpdateUiState.enabled == false` (updater unreachable);
   * **`AppContainer.getInstance(context)` builds** the real DI graph (token store, cookie jar, network module, all repositories) against release code;
   * one fixture decoded per DTO family with the app's own `Json` — `UserDto`, `NotificationListResponse`, `SavedProfessionalItemDto`, `BookingDto`, `AiUsage`.
3. **Dex inspection step** — unzips the release APK, dumps the dex and asserts `$$serializer` symbols and `ServixApiService` survived R8 (`serializer refs`, `api refs` printed in the log), so the keep rules are proven on the *minified* bytecode, not just the compiled classes.
4. Size signal: debug APK 23.8 MB → minified release APK **3.46 MB**.
5. APK signature verified with `apksigner --print-certs`: `cf6460a5e679cd797779551af48b1bf9dd8dda8f524a14db55489ae49717299d` (test key), so it installs over the current test builds.

## Known caveat

R8 in AGP 8.13 parses Kotlin 2.4 metadata with warnings ("newer version of kotlin than … this version of R8"). They are warnings only — the build is green and the dex checks pass — but they would disappear by pinning Kotlin to the 2.3.x line or moving to AGP 9.x. Worth revisiting before the first production update.

## Artifacts

`v1.0.0-b48` pre-release carries both `servix-1.0.0-build48.apk` (debug, self-updating) and **`servix-1.0.0-release-build48.apk`** (minified release), and the workflow uploads `servix-1.0.0-mapping-build48.txt` as an artifact for de-obfuscating Play crash reports.

## Next block

Block 3 (separate upload keystore) waits for "go".
