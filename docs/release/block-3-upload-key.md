# Release prep — Block 3: separate Play upload key

**No secret material is stored in this repository.** The base64 keystore and its password were printed
once in the chat report for the owner to paste into GitHub secrets and a password manager, and the
sandbox copies were deleted immediately after.

## What was generated

| Property | Value |
| --- | --- |
| File | `servix-upload.p12` (PKCS12) |
| Alias | `servix-upload` |
| Key | RSA **4096**, 30-year validity |
| Subject | `CN=Servix Upload Key, O=Servix` |
| Valid from / to | 2026-11-05 → **2056-10-29** |
| Certificate SHA-256 | `BC:31:4D:57:84:2B:A6:BF:FE:D5:77:11:EB:13:78:FA:1C:BC:6B:0A:52:A9:1F:1A:F6:82:16:12:15:B0:2D:94` |

Generated with `cryptography` (PKCS12 + self-signed X.509), verified by re-opening it with OpenSSL and
printing the subject, validity window and SHA-256 fingerprint above.

## What it is (and is not)

With **Play App Signing**, Google holds the *app signing key* that actually signs what users install.
This keystore is the **upload key**: it only signs the `.aab` we hand to Play Console, which is how
Google verifies that uploads come from us. Consequences:

* Losing it can be recovered by Google's upload-key reset flow, but it is still worth storing safely.
* Leaking it does not let anyone publish to users directly — they would also need Play Console access.
* It is deliberately **different** from the test key (`servix-test`, RSA 2048) that signs CI APKs, so a
  leak of one never implicates the other.

## GitHub secrets to create

| Secret | Value |
| --- | --- |
| `UPLOAD_KEYSTORE_BASE64` | the base64 blob printed in the report |
| `UPLOAD_KEYSTORE_PASSWORD` | the password printed in the report |

## Wiring in the build

`app/build.gradle.kts` now declares a `playUpload` signing config that is created only when
`SERVIX_UPLOAD_KEYSTORE` (path) and `SERVIX_UPLOAD_KEYSTORE_PASSWORD` are present in the environment;
CI will export those two from the secrets above. The **release** build type picks

1. `playUpload` when the upload secrets exist,
2. otherwise the existing shared `testRelease` key,

so the debug-APK workflow keeps building green before the secrets are added. Block 4's
`android-release.yml` will decode `UPLOAD_KEYSTORE_BASE64` to a temp file, export the env vars, build
the signed `.aab` and print its certificate fingerprint.
