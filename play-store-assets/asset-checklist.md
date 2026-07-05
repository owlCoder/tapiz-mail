# Tapiz Mail — Play Console Asset Checklist

Status snapshot taken 2026-07-05. `play-store-assets/` for Tapiz Mail is currently
**empty** (only `listing.md` and this file exist, both text). Nothing visual has been
generated yet — everything below is still needed.

Reference: `tapiz-boards/android/play-store-assets/` already has a complete first pass
(icon + feature graphic, generated via a Pillow script) and is used here as the model
for both the required spec and the generation approach.

## Requirements (verified against Play Console docs, July 2026 — re-verify at submission
time in case Google has changed anything since)

| Asset | Required? | Spec | Format |
|---|---|---|---|
| App icon | **Required** | 512 x 512 px, full-bleed (no pre-rounded corners — Play applies rounding) | 32-bit PNG **with alpha**, ≤1 MB |
| Feature graphic | **Required** | 1024 x 500 px | JPEG or 24-bit PNG, **no alpha**, ≤1 MB |
| Phone screenshots | **Required, minimum 2** (3–8 recommended) | 320px–3840px on the long side, standard is 1080x1920 (9:16) or 1920x1080 (16:9); max aspect ratio 2:1 | JPEG or 24-bit PNG, no alpha, ≤8 MB each |
| 7-inch tablet screenshots | Optional | Same pixel/format rules as phone | JPEG or 24-bit PNG |
| 10-inch tablet screenshots | Optional | Same pixel/format rules as phone | JPEG or 24-bit PNG |
| Promo video | Optional | YouTube URL (not an uploaded file) — feature graphic doubles as its thumbnail overlay, with a circular play button centered on it | — |

Notes:
- Google dynamically rounds the icon's corners at display time — submit a full square,
  not a pre-rounded shape, or you'll get double-rounding/visible artifacting.
- Feature graphic must NOT have an alpha channel (opposite of the icon) and should keep
  key content away from the edges — some surfaces crop/clip corners, and if a promo
  video exists a circular play icon sits centered on top of it.
- Screenshots must not contain device frames added by Play Console itself if you also
  add your own frame mockup — either is fine standalone, just don't double-frame.

## What Tapiz Mail currently has vs. needs

| Asset | Status |
|---|---|
| App icon (512x512 PNG) | **Missing** — needs generation |
| Feature graphic (1024x500) | **Missing** — needs generation |
| Phone screenshots (min. 2) | **Missing** — needs real device/emulator captures of Inbox, Add Account, Categories, and Compose screens (the four most representative flows) |
| Tablet screenshots | Not present — optional, skip for initial submission unless the app is tablet-optimized |
| Promo video | Not present — optional, skip for initial submission |
| Asset generator script (`generate_assets.py` equivalent) | **Missing** — not written in this pass (see blocker below) |

Confirmed by listing `tapiz-mail/play-store-assets/` directly: it contained nothing
before this task; `listing.md` and this checklist are the only files added.

## Reference: what a complete set looks like (from `tapiz-boards`)

`tapiz-boards/android/play-store-assets/` currently has:
- `generate_assets.py` — a Pillow script that procedurally draws the icon and feature
  graphic from the brand's color/logo recipe (gradient background, the product's
  LogoMark bars redrawn as vector shapes via `ImageDraw`, brand wordmark + tagline text
  via a system font). No external design tool or human-drawn asset needed.
- `tapiz-boards-icon-512.png` — generated output of the above.
- `tapiz-boards-feature-1024x500.png` — generated output of the above.
- Tablet screenshots and a promo video are **not** present there either — Boards' own
  set is icon + feature graphic only, no screenshots yet. So even the "reference"
  set is partial; Tapiz Mail needs at minimum the same two graphics plus the phone
  screenshots Boards hasn't produced yet either.

## Recommended approach for Tapiz Mail

1. **Icon + feature graphic**: follow the same Pillow-script pattern as
   `tapiz-boards/android/play-store-assets/generate_assets.py` — a `generate_assets.py`
   in this folder that draws Tapiz Mail's brand mark (the envelope glyph currently
   living only as the Android drawable `ic_launcher_foreground.xml` / `splash_logo.xml`,
   per `tapiz-mail/CLAUDE.md`'s "Nedovršeno" section) on a brand-gradient background,
   plus a feature graphic with the wordmark "Tapiz Mail" and a short tagline (e.g. "All
   your mail, one inbox." — the existing onboarding tagline is a natural fit here too).
   **Blocker**: this requires Python + Pillow (`pip install Pillow`) available in the
   environment. Not confirmed installed here, and out of scope for this text-only task
   — flagging so whoever runs the script checks/installs it first
   (`python -c "import PIL"` to verify).
2. **Screenshots**: require running the actual app (emulator or device) and capturing
   the Inbox (with categories visible), Add Account, and Compose screens at minimum —
   out of scope for this task since it needs a build/run, not just asset prep. Suggest
   capturing directly at 1080x1920 (or your device's native resolution, Play Console
   will accept anything 320–3840px) with no manual cropping needed if within range.
3. Do NOT reuse Boards' brand colors/logo — Tapiz Mail should draw from its own
   `ui/theme/TapizColors.kt` / `AppColors` token values and the envelope glyph, not the
   Boards 3-bar mark, to keep each product's Play Store presence visually distinct
   consistent with its own in-app branding.
