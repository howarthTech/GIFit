# GIFit — Status

## Status
live — https://play.google.com/store/apps/details?id=com.gifit.app

## Recently shipped
- **LIVE ON GOOGLE PLAY (2026-07-28)** — 1.1 (versionCode 3) passed production review and is
  publicly rolled out. Listing verified resolving (HTTP 200). First public release: includes
  the stale-frames fix, 16 overlay fonts, WYSIWYG outline text, and the in-app version label.
- **Portfolio pack dropped** (2026-07-12) at
  `Howarth Tech Solutions\_Company\marketing\portfolio-inbox\gifit\` — pack.md (Overview /
  Key features / Technical highlights / What it demonstrates), 2880×1620 hero, OG image,
  512 icon, 4 raw phone screenshots. Marked `Status: draft`; flip to `ready` + drop the
  draft note in pack.md once the Play listing is live.
- **1.1 (versionCode 3)** (2026-07-12): overlay fonts expanded 4 → 16 device system font
  families (casual, cursive, small caps, typewriter, weights, italics — all offline); removed
  the translucent background chip behind preview text and replaced it with the same
  outline-over-fill the encoder bakes, so the preview is now pixel-faithful WYSIWYG. Signed
  AAB at `dist/GIFit-release-v1.1-vc3.aab` — upload this into the Play Console Production
  draft ("Untitled release") and Start rollout. Note: the earlier 1.0.1 (vc2) production
  submission never actually went out — the draft was created without a bundle and was never
  sent for review; vc3 supersedes it.
- **1.0.1 (versionCode 2) — fixed stale-frames bug** (2026-07-12): starting a new GIF after
  clearing images kept showing the previous project's frames in preview (activity-scoped
  PreviewViewModel + an `if (frames.isEmpty())` reload guard). Fixed the reload trigger, cleared
  prior frames/generated GIF on load, and clamped the preview index. Verified on-device. Signed
  AAB built (`GIFit-release-v1.0.1-vc2.aab`). This is the bundle to publish — not the vc1 one.
- **Published to Google Play Closed Testing (Alpha) on 2026-07-09** — signed AAB
  (versionCode 1, 1.0), store listing, content rating, and Data Safety all accepted by review.
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
- Monitor first public installs/ratings in Play Console; respond to any user reviews.
- Confirm the stale-frames-bug tester gets 1.1 via the Alpha track and re-verifies the fix.
- Marketing to add the now-live Play Store link to portfolio-gifit.html (comms sent 2026-07-28).
- Feature roadmap (loop count selector, speed presets, sticker overlays, etc.) is tracked in
  `.claude/plan.md`.

## Needs Darrell
- **Two Play Console grants to finish the metrics wiring** (decision made 2026-07-28: collector
  extended with a Play-installs source; code + registry entry are live, GIFit already writes an
  honest row with `installs` declared missing until these are done):
  1. Play Console → **Download reports** → "Copy Cloud Storage URI" → paste the bucket name
     (the `pubsite_prod_...` part, no `gs://`) into `_play_bucket` in
     `_Company\analytics\registry.json`.
  2. Play Console → **Users and permissions** → invite
     `hts-analytics-collector@howarth-tech-solutions.iam.gserviceaccount.com` with
     **"View app information and download bulk reports"** (read-only).
  Full steps: `_Company\docs\10-analytics-collector.md` § "Play installs source". Note: exports
  lag ~2 days, and a just-launched app has no CSV until first data lands — so `installs` may
  stay declared-missing for a few days even after the grants.

## Notes
- No backend, no network permissions, no analytics/ads SDKs — nothing to configure server-side.
- App is not yet hosted anywhere (it's a local APK/AAB, not a VPS-hosted product) — no Ops/infra
  note needed for this round of changes.
