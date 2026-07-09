# GIFit — Status

## Status
in build — live on Google Play Closed Testing (Alpha); not yet in production

## Recently shipped
- **Published to Google Play Closed Testing (Alpha) on 2026-07-09** — signed AAB
  (versionCode 1, 1.0), store listing, content rating, and Data Safety all accepted by review.
  Next: add testers + opt-in link, shake it down, then promote the same bundle to Production.
- Reconciled a month-long divergence between `main` and an abandoned `master` branch (both had
  independently built overlay-text systems and independent "remove dead service / sign release"
  cleanups). Kept `main` — the more mature line — as the base, ported forward `master`'s clean,
  non-overlapping wins (new app icon, keyboard fix, build-output redirect, 10s delay sliders).
  See `.claude/plan.md` for the full reconciliation notes.
- Confirmed `main` already had the dead-service/permission cleanup and a release signing config;
  added `isShrinkResources = true` on top of the existing R8 minification.
- Added `OutOfMemoryError` handling around image loading and GIF generation in
  `PreviewViewModel`, with a user-facing message suggesting fewer frames / lower resolution,
  instead of a hard crash on large batches.
- Added `strings.xml` (`app_name`) and pointed the manifest label at it instead of a hardcoded
  string.
- Drafted `PRIVACY_POLICY.md` and a styled `docs/privacy-policy.html` for GitHub Pages hosting.
- Release keystore (`gifit-release.jks`, repo root, gitignored) generated, backed up externally,
  and verified against a real signed `assembleRelease` build.
- Published the privacy policy: GitHub Pages enabled on `main` serving `/docs`, live at
  https://howarthtech.github.io/GIFit/privacy-policy.html

## Next
- **Play Console setup**: create the app listing, use the privacy policy URL above, fill out
  Data Safety (answer: no data
  collected/shared), content rating questionnaire, and a store listing (screenshots, short/full
  description, feature graphic).
- Bump `versionCode`/`versionName` in `app/build.gradle.kts` when actually cutting the first
  release build for upload.
- Everything else on the feature roadmap is tracked in `.claude/plan.md`.

## Notes
- No backend, no network permissions, no analytics/ads SDKs — nothing to configure server-side.
- App is not yet hosted anywhere (it's a local APK/AAB, not a VPS-hosted product) — no Ops/infra
  note needed for this round of changes.
