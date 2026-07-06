# ClearCart Agent Guide

## Project

ClearCart is a native Android app inspired by product scanner apps, but it must not copy Yuka branding, UI, wording, assets, layout, or scoring formula.

The app helps users scan food, drink, cosmetics, and household products before buying. It should emphasize transparency, personalization, data confidence, practical shopping decisions, local-first privacy, and calm explanations.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room for local scan history and saved products
- Retrofit for product API structure
- CameraX and ML Kit Barcode Scanning for barcode scanning
- ML Kit Text Recognition structure for label/OCR fallback

## Build And Verify

Use the local Android toolchain already configured in this workspace.

On Windows PowerShell:

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleDebug
```

The debug APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Product Rules

- No account required for MVP.
- No ads.
- No tracking.
- Local-first by default.
- Product lookup may send barcode values to external product databases such as Open Food Facts.
- Manual product entry must remain local unless a later feature explicitly adds upload behavior.
- Include clear data attribution for public/open data sources.

## Health And Safety Wording

Do not make medical claims.

Avoid wording such as:

- safe
- dangerous
- toxic
- will prevent disease
- medically recommended

Prefer wording such as:

- may be worth limiting
- flagged based on your preferences
- higher sugar than similar products
- ingredient data is incomplete
- check with a professional for medical dietary needs

## UI Expectations

- Keep the UI modern, calm, and trust-building.
- Use a clean light background with restrained accents.
- Do not make the app look like a Yuka clone.
- Keep scanner preview height controlled.
- Do not let the camera preview destroy layout.
- Keep buttons outside the camera preview unless they are tiny overlay controls.
- Use vertical scrolling where screens may overflow.
- Avoid horizontal clipping on small phones.
- Keep bottom actions clear of the navigation bar.

## Architecture

Keep boundaries modular:

- `data/api/` for external product APIs.
- `data/db/` for Room persistence.
- `data/model/` for app models.
- `data/repository/` for data orchestration and mock fallback.
- `domain/scoring/` for explainable score logic.
- `domain/confidence/` for data confidence logic.
- `domain/preferences/` for user preference persistence and matching.
- `domain/alternatives/` for alternative suggestions.
- `scanner/` for barcode scanning.
- `ocr/` for label text extraction structure.
- `ui/screens/` for screen-level Compose UI.
- `ui/components/` for reusable UI pieces.
- `ui/theme/` for app theme.

## Scoring Rules

- Keep scoring explainable and modular.
- Do not copy Yuka's scoring formula.
- Preserve separate subscores and show the breakdown clearly.
- Let preferences influence warnings, suggestions, and recommendation text.
- Do not hide raw facts because of preferences.
- Show confidence prominently on product result screens.

## Current MVP Scope

Prioritize a working MVP before advanced features:

1. Scanner
2. Product lookup
3. Result screen
4. Score explanation
5. Preferences
6. Local history
7. Manual entry
8. Basic OCR fallback structure
9. Comparison
10. Mock data for testing

## Publishing Notes

When publishing to GitHub:

- Do not commit generated build output such as `app/build/`.
- Do not commit local-only files containing machine paths unless intentionally needed.
- Prefer a clean commit message such as `Build ClearCart MVP`.
- Open a draft PR unless the user explicitly asks for ready-for-review.
