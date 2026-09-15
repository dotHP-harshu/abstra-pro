# Abstra Pro — Product Requirements Document
**A new, standalone Android music player. Abstra's design language. Lune's feature depth. Its own codebase, its own identity.**

| | |
|---|---|
| **Doc owner** | You (dotHP-harshu) |
| **Status** | Draft v1.0 |
| **Working name** | "Abstra Pro" |
| **Relationship to Abstra** | New app. Not a fork, not a variant. You may reuse your own Abstra source (you own the MIT copyright) as a starting point for UI code. |
| **Relationship to Lune** | Feature/behavior spec only. Lune's source is GPLv3 — see §3 before writing any code. |
| **Date** | 2026-09-15 |

---

## 1. Summary

This is a **new music player app**, separate from both Abstra and Lune, distributed under its own name and package id. It is not an update to Abstra and does not replace it — Abstra keeps existing as-is.

The new app's mandate:
- **Visual identity = Abstra's**, rebuilt fresh: palette-driven album art theming, the vinyl-record player centerpiece, animated swipeable segmented tabs, marquee text, Abstra's Material 3 theme system, its bottom-sheet/dialog language, its illustration style.
- **Feature depth = Lune's**, reimplemented from documented behavior: 10-band EQ + full advanced audio-FX suite, AI-driven mixes, a home-screen widget with premium blur, synced lyrics, deep customization, playlist backup/export, multi-language, a resume/home screen.
- **Codebase = new.** You may reuse your own Abstra Compose components (you hold MIT copyright on them) as the visual foundation. Lune's code is not to be copied — see licensing section below — its features are re-engineered from scratch against the same public Android APIs Lune itself uses (`DynamicsProcessing`, `LoudnessEnhancer`, `EnvironmentalReverb`, `RenderEffect`, etc.).

### 1.1 Naming
"Abstra Pro" is the name of the app.

---

## 2. Why a separate app (not a merge into Abstra)

- Abstra stays a lightweight, focused player for users who want exactly what v2.0.0 offers — no scope creep, no feature bloat, no risk to its existing stability or install size (~12–18 MB today).
- The new app can carry a heavier feature set, more permissions (e.g. AppWidget provider), more Room tables, more settings screens, without that trade-off touching Abstra's users.
- Two separate listings let you position them differently: Abstra as "minimal & fast," Abstra Pro as "everything, still fast."
- Engineering-wise, cleaner: no need to gate advanced features behind flags inside one app, no risk of Abstra's `AppDatabase` migrations getting more complex than they need to be.

---

## 3. Licensing — read this before writing any code

| App | License | What it means for you |
|---|---|---|
| **Abstra** | MIT (you are the copyright holder) | You can copy, relicense, or rebuild your own Abstra code in the new app however you like. No restriction. |
| **Lune** | **GNU GPLv3** | GPLv3 is copyleft. If Abstra Pro's code is a **derivative work** of Lune (i.e., you copy/adapt Lune's actual source files), Abstra Pro **must** also be licensed under GPLv3 (or a compatible license) and you must make its full source available. |

**Two valid paths — pick one deliberately:**

1. **Clean-room reimplementation (recommended, keeps you license-flexible):** Treat this PRD's feature descriptions (§6) as the spec. Implement each feature against the underlying Android APIs (`DynamicsProcessing`, `LoudnessEnhancer`, `PresetReverb`/`EnvironmentalReverb`, `Virtualizer`, `PlaybackParams`, `RenderEffect`, Room, `jaudiotagger`) without reading/copying Lune's `.kt` files line-by-line. This lets Abstra Pro stay MIT (or any license you want), matching Abstra.
2. **Direct adoption of Lune's code:** If it's faster to port real chunks of Lune's `audio/*.kt` or `LuneWidgetProvider.kt`, that's allowed under GPLv3 — but then **Abstra Pro as a whole must ship as GPLv3**, with full attributed source published. This is a legitimate, common choice (Lune itself is proudly GPL/F-Droid-style open source) — just make it consciously, not by accident.

This PRD is written so either path works: all feature specs below describe *behavior*, not implementation lifted from Lune's files.

---

## 4. Goals

1. Ship a standalone app that plays every format Lune plays, with equal or better audio-processing depth.
2. Every screen looks like a natural evolution of Abstra's design system — same corner radii, palette-gradient theming, motion language, bottom sheets, dialogs, illustrations.
3. Privacy parity with Lune: no network permission, no telemetry, fully offline, on-device AI only.
4. A deliberate, documented licensing choice (§3) made before Phase 1, not discovered during a Play Store review.
5. Independent release cadence from Abstra — its own version number, its own changelog, its own Play Store/F-Droid listing.

## 5. Non-Goals

- Not modifying Abstra's existing repo/app in any way.
- Not streaming/cloud accounts, ads, or analytics SDKs.
- Not a 1:1 visual or code copy of Lune.
- No tablet/desktop-specific redesign in this phase.

---

## 6. Feature specification (the Lune-derived feature set, described independent of Lune's source)

### 6.1 Playback Engine
- Core playback via AndroidX Media3 (`ExoPlayer` or `MediaPlayer`, your call — Lune uses `MediaPlayer`, Abstra uses Media 1.7 + MediaSession; Media3 `ExoPlayer` is the more future-proof pick for a new 2026 app and handles gapless/crossfade transitions more cleanly).
- Formats: MP3, AAC/M4A, FLAC, OGG Vorbis, Opus, WAV, ALAC, AMR, MIDI, MP2.
- Dual-tier queue: explicit "Play Next" queue + underlying context queue (album/playlist/folder) — carried over from Abstra's proven pattern.
- Smart previous button: double-tap (~1.2s window) skips back a track; single tap restarts current track if >3s played.
- Automix/Crossfade: ~12-second configurable cross-transition between tracks.
- Smooth fade-out (~200ms) on `ACTION_AUDIO_BECOMING_NOISY` (headphone/Bluetooth disconnect).
- Sleep timer: Off / 15m / 30m / 60m, with a visible countdown chip in the full player.

### 6.2 Advanced Audio Effects Suite
All implemented against Android's public audio-effects APIs, exposed through one Equalizer screen with sub-sections/tabs (Abstra-styled):
- **10-band EQ** with presets.
- **Bass Boost** — independent hardware-level offset.
- **Virtualizer / Spatial Audio** — smooth ramp on toggle.
- **Loudness Enhancer** — 0–30 dB gain slider.
- **Balance** — L/C/R stereo pan, live indicator, instant-reset button, persists across track transitions.
- **Reverb** — `EnvironmentalReverb` (API 31+) with `PresetReverb` fallback; 6 environments (Room, Hall, Plate, Stage, Arena, Cathedral).
- **Dynamics Processor** — multi-band compression + limiter, presets: Light (1.5:1), Medium (3:1), Strong (5:1), Night (8:1).
- **Pitch/Speed** — combined 0.5×–2.0× control via `PlaybackParams`.

### 6.3 Visualizer
Rhythm-reactive bar/waveform display rendered as a Compose `Canvas`, skinned with the current track's palette-extracted colors (not a flat accent) — configurable via a bottom sheet (style, sensitivity, on/off).

### 6.4 Synced Lyrics
Local `.lrc`-file lookup (same-directory or embedded), synced scrolling with current-line highlight, reached from the full player (swipe/icon, not a separate Activity), background using the same palette gradient as the current album art. Empty state uses house illustration style.

### 6.5 Home-Screen Widget
`AppWidgetProvider`-based widget with a **premium blur background**: `RenderEffect`-based blur on Android 12+, a software-blur fallback below API 31. Live album art, play/pause/skip controls, tap-to-open. One consolidated in-app **Widget Settings** screen (cover shape, blur strength, control layout, accent color) rather than several separate screens.

### 6.6 Resume / Home Tab
A dedicated first tab (alongside Songs / Playlists / Folders), swipeable via the same segmented-tab + pager pattern:
- **Hero card** — "Continue Listening," large palette-gradient treatment matching the full player.
- **Recently Added** — horizontal scroll row.
- **Playlist grid**.
- **Recommendations** (see AI Mixes, §6.7).
- **Top Artists / Top Genres** — derived from local play-count/listening-time stats.

### 6.7 AI Mixes (on-device only)
Auto-generated themed playlists ("Most Played," "Recently Loved," "Genre: X," "Forgotten Favorites") computed **entirely on-device** from local play history — no network calls, no cloud model. v1: rule/heuristic-based over Room-tracked play counts, recency, favorites, genre/artist tags. v2 (stretch): on-device similarity model, still offline.

### 6.8 Gesture Customization
Configurable double-tap / swipe / long-press actions on mini-player and full-player, as a section inside Settings (not a separate Activity).

### 6.9 Folder Browser
Full nested-directory navigation (not just a flat folder-as-category list), with breadcrumb-style navigation using the segmented-tab visual pattern.

### 6.10 Playlist Backup & Export
Export/import playlists as a portable JSON format via Storage Access Framework, with confirmation dialogs in-house-styled.

### 6.11 Queue Management & Reordering
A queue bottom sheet showing the live play queue with drag-to-reorder and swipe-to-remove.

### 6.12 Search
Sectioned results: songs, favorites, albums, playlists, tag matches — from a top-bar search entry point.

### 6.13 Metadata Editing
Two explicit modes exposed to the user: "App-only override" (safe default, database-level only) vs. "Write to file tags" (opt-in, uses a tagging library against the public file format, e.g. via `jaudiotagger` or an equivalent you implement/license independently).

### 6.14 Multi-language Support
Ship translated `strings.xml` for a chosen locale set (recommend starting with your actual/expected user base rather than porting all nine of Lune's locales day one — see open questions).

### 6.15 Custom App Title
Simple settings field.

### 6.16 Privacy Posture
No `INTERNET` permission. No telemetry, no crash reporters, no ads. Fully offline. A short in-app Privacy statement in Settings/About, matching this posture explicitly.


## 7. UI/UX system — rebuilt from Abstra's design language

Since this is a new codebase, the design system should be **extracted into a reusable internal library of composables**, mirroring what already works in Abstra, so every net-new screen inherits it automatically instead of being restyled by hand each time:

| Design primitive | Rebuilt as | Applied across |
|---|---|---|
| Palette-driven album art theming | `ThematicAlbumArt`-equivalent: decode embedded art, extract dominant/vibrant/muted colors, build gradient theme; deterministic hashed-gradient fallback for art-less tracks | Full player, Lyrics screen background, Widget preview, AI Mix cards, Resume Hero |
| Vinyl-record centerpiece | GPU-only rotation via `graphicsLayer { rotationZ }`, no recomposition cost | Full player; optional centerpiece on Lyrics screen when lyrics are off/unavailable |
| Dialog pattern | Theme-aware destructive color, consistent copy/button layout | Delete playlist, clear cache, reset EQ |
| Illustration set | Custom empty-state art, not stock icons | No lyrics found, empty AI mix, empty search, first-run screens |
| Material 3 theme tokens | Central `AppColors`/`AppTheme` object | Entire app, including widget settings screen |

**Design rule:** any new feature screen must be built entirely from this shared component set. If a feature seems to need a new visual primitive, add it to the shared library first — don't one-off style a screen.

---

## 8. Information architecture

```
Bottom/Top Nav
├─ Home       (Hero, Recently Added, Playlist Grid, AI Mixes, Top Artists, Top Genres)
├─ Songs
├─ Playlists  (+ Export/Import entry point)
└─ Folders    (nested browsing)

Full Player
├─ Vinyl / ThematicAlbumArt centerpiece
├─ Visualizer overlay (palette-skinned)
├─ Lyrics (swipe-accessible screen)
├─ Queue bottom sheet (reorderable)
├─ Effects: EQ + Loudness + Balance + Reverb + Dynamics + Pitch
└─ Sleep timer chip

Settings
├─ Playback: Automix/Crossfade, Gesture customization
├─ Audio: → Equalizer screen
├─ Widget: consolidated customization screen
├─ Library: Export/Import playlists, tag-write toggle
├─ Appearance: theme + Language
└─ About / Privacy

Search (top-bar entry point)
Home-screen Widget (AppWidgetProvider)
```

---

## 9. Technical architecture

- **Language/UI:** Kotlin, Jetpack Compose, Material 3.
- **Architecture:** MVVM + StateFlow (proven in both source apps).
- **Playback:** Media3 `ExoPlayer` + `MediaSession` (recommended over legacy `MediaPlayer`/`android.media.session` for a fresh 2026 codebase — better gapless/crossfade support, actively maintained).
- **Persistence:** Room, single `AppDatabase`, explicit `Migration` objects from day one (don't destructive-migrate).
- **Audio FX:** `android.media.audiofx` (`Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`, `PresetReverb`/`EnvironmentalReverb`, `DynamicsProcessing`, `PlaybackParams`), wrapped in your own effect-manager classes (design fresh, don't port Lune's files — see §3).
- **Metadata:** `MediaMetadataRetriever` for read (as Abstra does) + a tagging library for opt-in write support.
- **Widget:** `AppWidgetProvider` + `RemoteViews`, `RenderEffect` blur (API 31+) with a manual software-blur fallback.
- **Caching:** bounded LRU caches with a `Semaphore`-limited concurrent-decode pattern (proven in Abstra's `EmbeddedArtCache`) for album art, lyrics, and any AI-mix intermediate data.
- **No new ML runtime dependency for v1** — AI Mixes v1 is pure Room-query heuristics.

### 9.1 Data model (Room tables, fresh schema — not migrated from either source app)

| Table | Purpose |
|---|---|
| `songs`, `albums`, `artists`, `folders` | Core library (MediaStore-backed, cached) |
| `song_overrides` | App-only metadata overrides (title/artist/album/genre/cover/favorite) |
| `playlists`, `playlist_songs` | Playlists |
| `playback_stats` | Per-song/artist/genre play counts, total time, last played |
| `queue_state` | Persisted current queue + explicit "Play Next" entries |
| `effect_settings` | Persisted EQ bands, balance, reverb env, dynamics preset, loudness gain, pitch/speed, crossfade duration |
| `lyrics_cache` | Parsed `.lrc`/embedded lyrics per song |
| `ai_mix_cache` | Generated mix definitions + refresh timestamp |
| `widget_prefs` | Widget cover shape, blur strength, control layout, accent |

---

## 10. Non-functional requirements

- **Performance:** foreground/background polling intervals tuned like Abstra's (200ms/1000ms) for battery. Visualizer rendering pauses when full player isn't visible.
- **Memory:** all new caches (lyrics, AI mix, art) follow the bounded-LRU + semaphore-limited pattern, not unbounded growth.
- **minSdk:** 24 (matches both source apps' user reach); graceful degradation below API 31 for blur/reverb.
- **Privacy:** no `INTERNET` permission in the manifest, period.
- **Build:** single Gradle module, Kotlin, KSP for Room.

---

## 11. Phased delivery plan

**Phase 0 — Foundation**
New repo, package id, app identity (name/icon), license decision (§3) finalized and documented in the repo's LICENSE file. Shared design-system component library extracted/rebuilt (theming, palette engine, vinyl centerpiece, tabs, marquee, sheets, dialogs, illustrations).

**Phase 1 — Playback core + Engine parity**
Media3-based playback engine, dual-tier queue, smart previous, noisy-disconnect fade, sleep timer, Automix/crossfade, full advanced audio-FX suite (EQ, Loudness, Balance, Reverb, Dynamics, Pitch, Bass Boost, Virtualizer), format verification across the full target list.

**Phase 2 — Core library screens**
Songs/Playlists/Folders (with nested browsing), Search, Queue reorder sheet, Playlist export/import, metadata editing (both modes).

**Phase 3 — Signature surfaces**
Full player with vinyl centerpiece + palette theming, Lyrics screen, Visualizer, procedural-audio empty state, playback stats screen.

**Phase 4 — Home & intelligence**
Home/Resume tab (Hero, Recently Added, Playlist Grid, Top Artists/Genres), AI Mixes v1.

**Phase 5 — Widget & polish**
Home-screen widget + consolidated customization screen, Gesture customization, multi-language rollout, theming polish pass, AI Mixes v2 (stretch).

---

## 12. Success metrics

- Every feature in §6 present and functioning, checked against this document as an acceptance list.
- Zero network permission in the shipped manifest.
- New-app cold start and list-scroll performance at or better than Abstra's current benchmarks.
- Visual QA: every new screen reviewed side-by-side against the shared design-system library for consistency (no one-off styling drift).
- Licensing decision (§3) documented and consistent across every source file's header/LICENSE.
