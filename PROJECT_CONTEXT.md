# ClearCart Project Context

## Repo

- GitHub: `https://github.com/BadBagger/clearcart`
- Android package: `com.clearcart.app`
- Latest APK release: `v0.1.7`

## Current State

ClearCart MVP has been scaffolded as a native Android Kotlin app using Jetpack Compose and Material 3.

The app currently includes:

- Home dashboard
- Barcode scanner screen with CameraX and ML Kit Barcode Scanning
- Dedicated product search screen for name, brand, category, or ingredient lookup
- Product search result cards with product thumbnails when source images are available
- Best by Category screen for top ClearCart-rated options from scanned, searched, cached, and sample products
- Manual barcode entry
- Normalized Product model with id, barcode, quantity/serving size, productType, dataSource, dataCompletenessScore, lastUpdated, and userEdited metadata
- Barcode lookup uses cached local product snapshots immediately before refreshing from API providers when possible
- Open Food Facts Retrofit API structure
- Modular product provider structure
- Open Beauty Facts Retrofit provider structure for cosmetics/personal care
- Mock fallback product data
- Product result screen
- Product result data-quality labels for Complete, Partial, Missing ingredients, Missing nutrition, User-added, and Needs review
- Plain-language ingredient explanations on product results with the full ingredient list, tap-to-explain ingredient rows, calm tags, and local avoid/okay ingredient controls
- Explainable scoring engine
- Explainable scoring output with separate ClearCart Score, Personal Fit, confidence score, top reasons, missing-data warnings, and preference matches/conflicts
- Confidence engine
- Preferences screen with DataStore-backed local persistence for food/drink, cosmetic, household, avoid-list, allergen, brand, and category preferences
- Room scan history
- Favorite, delete, clear, and search history actions
- Manual product entry with user-added data source metadata
- Basic OCR fallback screen and parser structure with parsed name/ingredient handoff into manual entry while preserving OCR data source metadata
- Product comparison screen
- Alternative suggestions that rank cached/mock candidates by category, product type, use case, score, Personal Fit, preference conflicts, package similarity, and data quality
- Explicit product selectors on the comparison screen
- Full product snapshot persistence for richer history/comparison
- Privacy screen
- Settings screen with future feature flags
- Focused unit tests for scoring, preferences, and confidence logic
- Ingredient explanation tests for preference-aware wording and non-medical language
- Ingredient tag tests for sweetener, preservative, coloring, thickener, fragrance, user-avoided, and user-favorite classifications
- Scoring tests for preference-separated personal fit, limited-data confidence behavior, and calm/non-fearmongering wording
- Personal Fit tests for lower-sugar/higher-protein matches, fragrance conflicts, and brand/category avoid lists
- Alternative suggestion tests for lower-sugar drinks, fragrance-free hair care, and weak-data rejection
- Category-best ranking tests for protein shakes and weak-data filtering
- Alcoholic beverage scoring regression coverage so beer is reviewed with alcohol context instead of treated like an ordinary low-sugar drink
- Repository regression coverage for reopening locally saved products when providers miss
- Product data quality tests for complete, missing-nutrition, manual, and OCR-reviewed products
- `ClearCartSummaryProvider` exposes a read-only Smithware Central summary at
  `content://com.clearcart.app.summary/summary` with scan count, favorites,
  average score, lower-confidence count, and recent product names only.

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

Release signing is configured through an ignored local `keystore.properties`
file using the release keystore under the user's Smithware signing folder.
Release APK output:

```text
app/build/outputs/apk/release/app-release.apk
```

## Latest Release

- Tag: `v0.1.7`
- URL: `https://github.com/BadBagger/clearcart/releases/tag/v0.1.7`
- Assets:
  - `ClearCart.apk`
  - `ClearCart-release-v0.1.7.apk`
- Release notes: Adds product thumbnails to search results, introduces Best by Category rankings including protein shakes, and adjusts alcoholic beverage scoring so beer is reviewed with alcohol context instead of treated like an ordinary low-sugar drink. Build, unit tests, release build, and APK signature verification passed.

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
- Alternatives currently use mock/cached product data only, with stronger matching and weak-data filtering.
- Compare screen now uses full cached product snapshots where available and lets users select the two scanned products to compare.
- ProductRepository now exposes `lookupByBarcode`, `lookupFoodProduct`, `lookupBeautyProduct`, `saveToCache`, `getCachedProduct`, `saveManualProduct`, and `updateUserCorrection`.
- Release signing is configured locally, and `v0.1.5` was the first release published with the Smithware release key.

## Suggested Next Steps

1. Add ClearCart to the DevHub registry/listing after deciding icon and preview assets.
2. Validate Open Beauty Facts lookup against real cosmetics/personal-care barcodes.
3. Wire live OCR camera capture with ML Kit Text Recognition.
4. Expand tests around provider fallback, Room snapshot persistence, and comparison tradeoff logic.
5. Add data attribution UI for Open Food Facts and Open Beauty Facts.

## Coordination Pattern

Use this file as a handoff document between Codex chats.

Future chats should:

- Read `AGENTS.md`.
- Read this file.
- Read the SoftSmith DevHub `AGENTS.md` and `PROJECT_CONTEXT.md` before publishing.
- Inspect current source files before editing.
- Update this file when project state, known gaps, build commands, or next steps materially change.
