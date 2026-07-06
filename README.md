# ClearCart

ClearCart is a native Android product scanner MVP built with Kotlin, Jetpack Compose, Material 3, Room, Retrofit, CameraX, and ML Kit.

It is inspired by the general product-scanner category, but it does not copy Yuka branding, layout, wording, assets, or scoring formula.

## What It Does

- Scans product barcodes with CameraX and ML Kit Barcode Scanning.
- Looks up food product data through an Open Food Facts provider structure.
- Includes an Open Beauty Facts provider structure for cosmetics and personal care expansion.
- Saves scan history locally with Room.
- Shows a calm ClearCart score, grade, confidence badge, and explainable breakdown.
- Lets users set preferences such as allergens, low sugar, low sodium, fragrance avoidance, and simpler ingredient lists.
- Supports manual product entry when barcode data is missing.
- Includes a basic OCR label fallback screen and parsing structure.
- Compares two scanned products using cached full product snapshots.

## Privacy

- No account required.
- No ads.
- No tracking.
- Scan history is stored on device.
- Product lookup may send a barcode to an external open product database.
- Manual product entries stay local in the MVP.

## Build

This workspace is configured for a local Windows Android toolchain.

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleDebug
```

Run focused unit tests:

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Mock Test Barcodes

```text
0123456789012
0099999999999
```

## Current Gaps

- Live camera OCR capture is not wired yet.
- Open Beauty Facts provider needs validation against real cosmetics and personal-care barcodes.
- Alternatives currently use mock/cached product data.
- Release signing is not configured.

## Coordination

Future Codex sessions should read:

- `AGENTS.md`
- `PROJECT_CONTEXT.md`

Use `PROJECT_CONTEXT.md` as the shared handoff/status file.
