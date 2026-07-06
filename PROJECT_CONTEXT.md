# ClearCart Project Context

## Repo

- GitHub: `https://github.com/BadBagger/clearcart`
- Android package: `com.clearcart.app`
- Latest APK release: `v0.1.2`

## Current State

ClearCart MVP has been scaffolded as a native Android Kotlin app using Jetpack Compose and Material 3.

The app currently includes:

- Home dashboard
- Barcode scanner screen with CameraX and ML Kit Barcode Scanning
- Dedicated product search screen for name, brand, category, or ingredient lookup
- Manual barcode entry
- Open Food Facts Retrofit API structure
- Modular product provider structure
- Open Beauty Facts Retrofit provider structure for cosmetics/personal care
- Mock fallback product data
- Product result screen
- Explainable scoring engine
- Confidence engine
- Preferences screen with local persistence
- Room scan history
- Favorite, delete, clear, and search history actions
- Manual product entry
- Basic OCR fallback screen and parser structure
- Product comparison screen
- Explicit product selectors on the comparison screen
- Full product snapshot persistence for richer history/comparison
- Privacy screen
- Settings screen with future feature flags
- Focused unit tests for scoring, preferences, and confidence logic

## Verified Build

The app compiled successfully with:

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleDebug
```

Focused tests also pass with:

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest
```

Output APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Latest Release

- Tag: `v0.1.2`
- URL: `https://github.com/BadBagger/clearcart/releases/tag/v0.1.2`
- Assets:
  - `ClearCart.apk`
  - `ClearCart-debug-v0.1.2.apk`
- Release notes: Adds a dedicated product search screen, text search provider support for Open Food Facts and Open Beauty Facts, and mock search fallback. Build and unit tests passed.

## Important Constraints

- Do not copy Yuka branding, UI, wording, assets, layout, or exact scoring formula.
- No medical claims.
- No fearmongering.
- No account required.
- No ads.
- No tracking.
- Local-first by default.
- Manual entries stay local in the MVP.
- Barcode lookup may contact Open Food Facts or another configured data provider.

## Test Barcodes

Mock fallback data includes:

```text
0123456789012
0099999999999
```

## DevHub

ClearCart is not connected to the DevHub Android app yet. To connect it, update the DevHub repo:

- `apps.yml`
- `android-app/app/src/main/AndroidManifest.xml`
- `android-app/app/src/main/java/com/softsmith/devhub/MainActivity.java`
- Store icon and preview assets
- DevHub `PROJECT_CONTEXT.md`

DevHub detects updates from GitHub Releases with APK assets attached. Source-only pushes do not update the app listing.

## Known Gaps

- Live camera OCR capture is not wired yet; the OCR screen currently supports editable text and parsing structure.
- Open Beauty Facts support now has a separate Retrofit provider structure, but needs broader real-world validation against cosmetics barcodes.
- Alternatives currently use mock/cached product data only.
- Compare screen now uses full cached product snapshots where available and lets users select the two scanned products to compare.
- Release signing is not configured; the current release uses the debug APK.

## Suggested Next Steps

1. Add a release signing config and produce a signed release APK.
2. Add ClearCart to the DevHub registry/listing after deciding icon and preview assets.
3. Validate Open Beauty Facts lookup against real cosmetics/personal-care barcodes.
4. Wire live OCR camera capture with ML Kit Text Recognition.
5. Expand tests around provider fallback, Room snapshot persistence, and comparison tradeoff logic.
6. Add data attribution UI for Open Food Facts and Open Beauty Facts.

## Coordination Pattern

Use this file as a handoff document between Codex chats.

Future chats should:

- Read `AGENTS.md`.
- Read this file.
- Read the SoftSmith DevHub `AGENTS.md` and `PROJECT_CONTEXT.md` before publishing.
- Inspect current source files before editing.
- Update this file when project state, known gaps, build commands, or next steps materially change.
