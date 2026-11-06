# Release prep — Block 4: the Play upload bundle workflow

**Nothing was published to Play.** There is no Play Publisher plugin, no service-account JSON and no
API call anywhere in this workflow. It builds a signed `.aab`, proves which key signed it, and parks
the file on a GitHub pre-release for the owner to upload by hand.

## What exists now

`.github/workflows/android-release.yml` — **`workflow_dispatch` only**. There is no `push` trigger, so
no commit can ever produce a Play-bound bundle by accident.

| Step | What it does | Fails the job when |
| --- | --- | --- |
| Restore Play upload keystore | Decodes `UPLOAD_KEYSTORE_BASE64` → `$RUNNER_TEMP/servix-upload.p12`, then opens it with `keytool -list -alias servix-upload` before Gradle runs; exports `SERVIX_UPLOAD_KEYSTORE` / `SERVIX_UPLOAD_KEYSTORE_PASSWORD` | a secret is missing/empty, the decode yields 0 bytes, or the password does not open the store |
| Read version | Greps `versionName` / `versionCode` from `app/build.gradle.kts` | either is absent (so the tag can never be malformed) |
| Release-variant smoke tests | `gradle testReleaseUnitTest --tests 'ng.name.servix.app.release.*'` — the Block 2 DI-graph + DTO suite, on the same release variant Play receives | any test fails |
| Build | `gradle :app:bundleRelease` (R8 + resource shrinking), log teed to `gradle-build.log` | the build fails |
| Verify the bundle | `unzip -l` the META-INF signature entries, echo `jarsigner -verify` verbatim (advisory), read the certificate with `keytool -printcert -jarfile` (falling back to `META-INF/*.RSA` + `keytool -printcert -file`), normalise to 64-char uppercase hex, assert it equals `BC:31:4D:57:…:2D:94`, and cross-check it against the certificate inside the restored keystore | no certificate is readable, the fingerprint does not match, or the keystore's own certificate is not the upload certificate |
| Name | `servix-1.0.0-play-build<N>.aab` + `servix-1.0.0-mapping-play-build<N>.txt`, plus `du -h` size and `sha256sum` of the bundle | `mapping.txt` is missing |
| Artifacts | Both files, 90-day retention | — |
| Pre-release | Tag `play-v<versionName>-b<run>`, `prerelease: true`, notes carrying the upload-key fingerprint, versionCode, bundle size, bundle SHA-256 and the line "Upload this .aab in Play Console → Testing → Internal testing" | — |
| Surface Gradle errors | On failure, writes filtered Gradle/jarsigner/keytool output to `docs/ci/last-failure.txt` and commits it | — |

The fingerprint gate is the point of the workflow. If the upload secrets were ever missing or wrong,
`signingConfig` would fall back to the shared **test** key (`testRelease`) — a bundle Play would
reject and, worse, a fingerprint mismatch the owner might not notice. The job now stops instead.

## Run history — it took three dispatches

### Run #1 — failed (owner-dispatched on `main`, run 42222040713)

Failed at "Verify the bundle exists and is signed by the upload key" with my own message
`The .aab is not correctly signed (jarsigner -verify failed). Play will reject it.`

The bundle was *not* in fact badly signed. Root cause: the check was written as

```bash
if ! echo "$SIGNERS" | grep -q "jar verified"; then   # under set -o pipefail
```

`grep -q` exits at the first match, `echo` then dies of `SIGPIPE`, and `pipefail` turns that into a
non-zero pipeline — so the "verified" branch was never taken. Runs #2 and #3 on the same signing path
show `jarsigner` verifying the bundle normally, which confirms the pipeline, not the signature, was
the failure. Fixed by writing `jarsigner` output to a file and grepping the file (commit `a22f0bf`),
which also made the step self-diagnosing: `jarsigner` is now **advisory** (its wording on bundle-style
jars is not a reliable pass/fail signal), the hard gates are "a certificate is readable" and "that
certificate is the upload key", and the step prints the META-INF listing plus both certificates.

### Run #2 — failed (run 42222970168)

The gate now read the right certificate off the bundle, then rejected it because of a formatting bug
of mine: `keytool` already prints the fingerprint colon-separated, and the normaliser re-inserted
colons (`BC::3:1::4D…`), so no comparison could ever succeed. The annotation is worth keeping as
evidence of what was actually read:

```
Bundle certificate mismatch. Expected BC:31:4D:…:2D:94 but got BC::3:1::4D::5:7::84::2:B::A6::B:F::FE::D:5::77::1:1::EB::1:3::78::F:A::1C::B:C::6B::0:A::52::A:9::1F::1:A::F6:8:2::16::1:2::15::B:0::2D::9:4
```

Fixed (commit `2da9a0a`) by stripping to canonical uppercase hex, asserting 64 characters, and
rebuilding the readable colon form from that. The parsing was verified locally against a real
`keytool` (Temurin 25, throwaway key): `len=64`, colon form intact.

### Run #3 — green (run 42223571175, commit `2da9a0a`)

The run's own verdict, quoted from its check-run notice annotation:

```
Bundle signed by the upload key BC:31:4D:57:84:2B:A6:BF:FE:D5:77:11:EB:13:78:FA:1C:BC:6B:0A:52:A9:1F:1A:F6:82:16:12:15:B0:2D:94
```

That string is emitted by the step only after both gates pass — the fingerprint read from the `.aab`
equals the expected upload certificate **and** the certificate inside the restored keystore equals it
too. It is byte-identical to the certificate created in Block 3.

Run: https://github.com/toluenetech-spec/servix-android/actions/runs/42223571175

## The hand-off artifact

| | |
| --- | --- |
| Pre-release | **https://github.com/toluenetech-spec/servix-android/releases/tag/play-v1.0.0-b3** |
| Bundle | `servix-1.0.0-play-build3.aab` — 7,153,970 bytes (6.9 MB) |
| Bundle SHA-256 | `d6af8c195216848014288492b576abc7543578a6708a43e8d146007fb2b3350a` |
| Mapping | `servix-1.0.0-mapping-play-build3.txt` — 57,596,804 bytes |
| versionName / versionCode | `1.0.0` / `100` |
| Upload-key certificate | `BC:31:4D:57:84:2B:A6:BF:FE:D5:77:11:EB:13:78:FA:1C:BC:6B:0A:52:A9:1F:1A:F6:82:16:12:15:B0:2D:94` |
| Trigger | `workflow_dispatch`, branch `arena/e9ca0c44-servix-android`, run #3 |

Both files are also attached to the run as a workflow artifact (`servix-1.0.0-play-build3.aab`,
90-day retention). Tag numbers are per-workflow: the release workflow's `b3` is its own third run and
has nothing to do with the APK workflow's `b50`.

## Facts the owner needs when uploading

* With **Play App Signing**, this fingerprint is the one Play Console shows under
  *App integrity → Upload key certificate*. It is **not** the fingerprint users' devices see — Google
  re-signs with the app signing key it holds.
* Keep `mapping.txt` for this exact bundle; Play needs it to deobfuscate crash reports.
* versionCode is `100`. Play refuses a second upload with the same versionCode, so the next bundle
  needs a bump in `app/build.gradle.kts` before dispatching again.
* The build has `SELF_UPDATE_CHANNEL = "none"` and is not debuggable; it will not try to update itself
  from GitHub.

## What did not happen

No Play Console call, no `com.github.triplet.play` plugin, no service account, no upload. The workflow
cannot publish even if someone wanted it to — it has no credentials for Play at all.

## Follow-ups added

* `jarsigner -verify` on a bundle only ever warns/advises here (see `docs/release/follow-ups.md`).
* Run logs are served from `results-receiver`/blob storage; when that is unreachable, check-run
  **annotations** (`gh api …/check-runs/<job>/annotations`) are the reliable channel for CI verdicts.
