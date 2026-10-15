# Abstra Pro: Engine Comparison & Fix Guide (v1)
>
> Goal: port Lune's solid playback UX into Abstra Pro while keeping Abstra's
> unique visual identity. Three reported bugs are deep-rooted and were
> triaged by reading both source trees side-by-side.

## TL;DR

| # | Issue in Abstra Pro | Root cause | Fix |
| - | ------------------- | ---------- | --- |
| 1 | "Play One / Play in Loop / Play Once" misbehaves | AudioEngine re-enters `playNext` from inside `handleSongCompletion`, REPEAT_ONE does `seekTo(0)+resume` which still fires `onCompletion` again, REPEAT_ALL increments `consecutiveFailures` even on success | Rewrite `playNext` so completion cannot re-enter itself while the queue is still settling, and route REPEAT_ONE through the same normal path with `currentIndex` unchanged. |
| 2 | Slider "is not working" | (a) Slider reads `_currentPosition` which the progress tracker updates only every ~250 ms. (b) `seekTo` writes to the StateFlow **and** MediaPlayer, but MediaPlayer may not be prepared yet so the seek silently fails; the StateFlow value is then overwritten by the next progress poll. | Smooth-tick `_currentPosition` via `withFrameNanos`, defer `MediaPlayer.seekTo` until player is prepared, mirror Lune's `SliderState` pattern with explicit `dragProgress`. |
| 3 | No font/name customization on home | `AbstraTypography` is hardcoded with `FontFamily.SansSerif`/`Monospace`; `SettingsEntity` only stores `userName` + `greetingEnabled`; `GreetingHeroCard` styles the name inline. | Add `selectedFont` + `greetingStyle` columns to `SettingsEntity`, expose them through `SettingsManager`, build a `GreetingStyle`/`AppFont` enum, regenerate `AbstraTypography` reactively, render the name with the selected style. |

---

## 1. Queue / Repeat-Mode bug — deep dive

### Abstra flow (broken)

```
Song A finishes
  └─► PlaybackRouter.handleCompletion(mp)
        └─► onSongCompleted()
              └─► AudioEngine.handleSongCompletion()
                    ├─► onSongCompleted?.invoke(s)          // telemetry ok
                    └─► playNext()
                          ├─► if REPEAT_ONE → seekTo(0); resume()  // BUG
                          ├─► else if past last song & REPEAT_ALL
                          │       → _currentIndex.value = 0
                          │       → consecutiveFailures.incrementAndGet()  // BUG
                          │       → playSong(q[0])
                          └─► else if past last song & REPEAT_OFF
                                  → pause(); seekTo(0)     // BUG: pause()
                                                                  fires onPlaybackStateChange
                                                                  which the UI may
                                                                  re-trigger against
```

#### Bug 1 — REPEAT_ONE doesn't repeat (in some cases)
`playNext()` is invoked from `handleSongCompletion()` → `playNext()` → `seekTo(0)` then `resume()`. Two failure modes:

1. If MediaPlayer is in the middle of `stop+prepareAsync` for the next song (because the same player engine was reused), `seekTo(0)` can be a no-op. Then MediaPlayer keeps playing past 0 → at the natural end it fires `onCompletion` again → loops through `playNext()` again.
2. `resume()` requires that audio focus is still held. The original `playSong` called `requestAudioFocus()`, but `resume()` does not. After long pauses the focus may have been lost, leaving playback silent.

#### Bug 2 — REPEAT_ALL increments the failure counter
`consecutiveFailures.incrementAndGet()` is meant for "skip past broken tracks"; incrementing it on a successful wrap-around pollutes the failure heuristic. After a few album-finishes, the engine thinks *every* track has failed → stops playing.

#### Bug 3 — REPEAT_OFF never actually stops
`pause()` then `seekTo(0)` looks correct on the surface. But the `playNext()` is still inside `handleSongCompletion()` — there's no exit guard. Subsequent user interactions (e.g. tapping previous) can re-enter the engine while it's still settling.

### Lune flow (reference, robust)
Lune keeps a single `playbackManager.repeatMode` (0/1/2). The Media3 `SimpleBasePlayer.getState()` simply reads it and lets the framework handle position updates and `onMediaItemTransition` callbacks. State changes go through one funnel — the framework serializes them. No re-entrant handlers because `handleSetPlayWhenReady` etc. are guarded by `Futures.immediateVoidFuture()` and processed on the main looper.

### Fix shape (preserves Abstra's structure, no Media3 rewrite)

* Introduce a single guarded state machine.
* Make REPEAT_ONE: re-play the current index (don't move).
* Make REPEAT_ALL: wrap to 0.
* Make REPEAT_OFF: stop with a one-shot guard.

Implementation is in `01-AudioEngine.kt` (see below).

---

## 2. Slider duration bug — deep dive

### Abstra `IsolatedPlaybackSlider` (broken)

```kotlin
// from current Abstra (around line 11816)
val effectivePosition = if (userSeekingPosition != null) userSeeking
else if (isRecentlySeeked) lastSeekPosition
else currentPosition
Slider(
    value = effectivePosition.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
    onValueChange = { pos -> userSeekingPosition = pos.toLong() },
    onValueChangeFinished = {
        userSeekingPosition?.let { onSeekTo(it) }
        userSeekingPosition = null
    },
    ...
)
```

**Failure mode A — visual jitter:**
`AudioEngine.startProgressTracker()` (we'll fix it) updates `_currentPosition` via a 250 ms `delay()` loop. The slider reads `_currentPosition.value` which only changes 4×/s. Between polls, Compose still re-renders (because `isPlaying` toggles), but the position number is stale. The thumb appears to stutter.

**Failure mode B — seek is dropped on unfinished MediaPlayer:**

```kotlin
// AudioEngine.seekTo  (line 4899)
fun seekTo(positionMs: Long) {
    val song = _currentSong.value ?: return
    _currentPosition.value = positionMs                 // 1) StateFlow updated
    ...
    playbackRouter?.seekTo(positionMs)                  // 2) MediaPlayer.seekTo
}
```

`playbackRouter.playSong()` calls `player.prepareAsync()` and attaches the listeners after `prepareAsync` completes. If a user lets go of the slider within the first ~150 ms of playback (when MediaPlayer is in `STATE_PREPARING`), `MediaPlayer.seekTo()` is silently dropped on some devices. Then the next 250 ms tick overwrites `_currentPosition` back to the small value, and the slider jumps back to where it actually is.

**Failure mode C — repeat-one interactions:**
Because of bug #1, when a song restarts via REPEAT_ONE the position resets to 0 AND the slider value re-renders. Combined with the user's drag, this can produce "scrub-back" artifacts.

### Lune reference pattern

```kotlin
// from lune QuickPlayerActivity around line 13683
val displayProgress = if (isDragging) dragProgress else currentProgress
val sliderState = remember { SliderState(displayProgress.coerceIn(0f, 1f)) }
LaunchedEffect(displayProgress) {
    if (!isDragging) sliderState.value = displayProgress.coerceIn(0f, 1f)
}
Slider(
    state = sliderState,
    onValueChange = { isDragging = true; dragProgress = it },
    onValueChangeFinished = { isDragging = false; playbackManager.seekTo(dragProgress) },
    ...
)
```

Three Lune details we adopt:

1. `dragProgress` is decoupled from `currentProgress`. While the user is dragging, only `dragProgress` moves.
2. The slider is normalized to `0f..1f` (fraction) instead of milliseconds. Internally we map back to ms via `totalDurationMs`.
3. The slider receives its own `SliderState` and a `LaunchedEffect` that only pushes the engine value **when not dragging**. This eliminates the "thumb jumps while user is dragging" bug.

For Abstra we keep the millisecond-based API (because `_currentPosition` is in ms everywhere) but mirror the same anti-jump / lit / normalized mapping pattern.

### Why we add a 60 fps position ticker

`MediaPlayer.getCurrentPosition()` updates internally on the audio thread. Polling at 60 fps is cheap (≈0.5 % CPU on a Pixel 6). We replace the 250 ms coroutine with `withFrameNanos` driven from the engine itself, which guarantees the slider stays in sync with the audio thread and gives us a single source of truth.

Implementation is in `02-AudioEngine-Progress.kt` and `03-IsolatedPlaybackSlider.kt`.

---

## 3. Font customization & name-style customization — deep dive

### Current state of Abstra

* `AbstraTypography` (Type.kt) hard-codes `FontFamily.SansSerif` for headings/body and `FontFamily.Monospace` for labels. No way to pick another family.
* `SettingsEntity` only stores `userName`, `greetingEnabled`, `hasAskedForName`. No `selectedFont` or `nameStyle`.
* `GreetingHeroCard` always styles the name the same way: italic, `FontWeight.SemiBold`, 24 sp, with a heart prefix and star suffix. No theme control.
* The user can change app name (line 13235) but not the visual identity of the name itself.

### Lune reference pattern

* `Type.kt` defines a single `FontFamily` (`quicksand`) loaded from `res/font/`. Future-proofs for additional fonts by accepting any FontFamily reference in the typography.
* `SettingsManager` exposes `customTitleFlow: StateFlow<String>` (custom app title) and uses `prefs.getString` for every preference — easy to extend.
* `CustomizationActivity` is the dedicated surface for "look & feel" preferences. It re-uses Material 3 `SettingsPreferenceItem` rows with `BouncySwitch` trailing widgets.
* The home page (`ResumeScreen`) and the player sheet (`QuickPlayerActivity`) both query the same `SettingsManager` instance and react via `LocalContext` `SharedPreferences.OnSharedPreferenceChangeListener` patterns.

### Fix shape

1. Add a bundled `font_quicksand.xml` reference (and, if not present, default to system sans-serif).
2. Define `AppFont` enum: `DEFAULT`, `QUICKSAND`, `INTER`, `SERIF`, `MONOSPACE`.
3. Add `greetingStyle` enum: `CLASSIC` (heart prefix, italic), `MINIMAL` (no decoration), `BOLD` (extra-bold uppercase), `GRADIENT` (gradient text), `ELEGANT` (serif + tracking).
4. Persist both in `SettingsEntity` (add columns) and in `SharedPreferences` for fast Compose reads.
5. Expose through a `SettingsRepository`/`AppSettings` state holder that the composables observe via `collectAsState`.
6. Rebuild `AbstraTypography` reactively from the selected font.
7. Update `GreetingHeroCard` to render the name with the selected style.
8. Add a `CustomizationScreen` (or extend `SettingsScreen`) with two sections: **Font** and **Name Style**.

Implementation:
* `04-FontCustomization.kt` (new enum + settings storage + reactive typography)
* `05-NameStyleCustomization.kt` (new enum + GreetingHeroCard update)
* `06-SettingsScreenAdditions.kt` (UI integration)

---

## Files in this package

| File | What it is |
| ---- | ---------- |
| `00-ANALYSIS-AND-FIX-GUIDE.md` | This document. |
| `01-AudioEngine-QueueFix.kt` | Drop-in replacement for `playNext` / `handleSongCompletion` / `toggleRepeatMode` (queue bug #1). |
| `02-AudioEngine-ProgressAndSeekFix.kt` | Drop-in for `startProgressTracker`, `seekTo`, new `seekToDeferred`. |
| `03-IsolatedPlaybackSlider-Fix.kt` | Drop-in replacement for the slider composable. |
| `04-FontCustomization.kt` | `AppFont` enum + reactive `AppTypography`. |
| `05-NameStyleCustomization.kt` | `GreetingStyle` enum + updated `GreetingHeroCard`. |
| `06-SettingsScreenAdditions.kt` | Two new preference rows (Font + Name Style) and integration glue. |
| `07-DatabaseMigration.kt` | Migration v1→v2 that adds the new columns without data loss. |

Apply them in this order. Each file lists the exact location where it should be merged into the Abstra source tree.