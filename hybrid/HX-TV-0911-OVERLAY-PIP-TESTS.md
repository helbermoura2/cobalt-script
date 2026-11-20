# HX TV 0.9.11 — PiP event sync + player suggestions dock

## Evidence from TCL P7K — video YouCut_20261120_183208326.mp4
- 0–17 s: Home, menu and category rows are functional, focus appears consistent.
- 18–27 s: long black loading screen with spinner. The 0.9.10 buffer message was **not visible**; the likely gap is stream preparation before ExoPlayer enters STATE_BUFFERING.
- 28–32 s: playback controls work visually, but remain dense.
- 33–35 s: suggestions fill most of screen even though their card thumbnails were made smaller.
- 37–45 s: PiP appears at upper-right over the Home; the safety dock added in 0.9.10 didn't appear to respond promptly.
- 46.5–48 s: second live begins considerably faster, so loading behavior is not uniform.

## Fixes in code
1. PiP: PlaybackActivity publishes entry/exit updates to a weakly referenced observer; HxHomeScreen subscribes on attachment and removes the observer on detach. The layout reserve is updated via main-thread `post`, in addition to focus/resume checks. Position remains controlled by the Android TV operating system.
2. Loading: PlaybackFragment displays "PREPARANDO VÍDEO..." if no first frame has appeared after 1,700ms, even if the stream hasn't reached ExoPlayer's buffering state. State is reset on every video change; message is removed after the first rendered frame. Cancels old tickets logically to prevent stale loading messages. Existing decoding, quality and networking unchanged.
3. Suggestions: when suggestions are selected, only the bottom 46% (minimum 260dp) of the Leanback controls dock is used, restoring MATCH_PARENT when returning to regular controls. D-pad rows and all items remain available; viewport becomes scrollable rather than occupying full-screen. Cards remain 146×82dp.
4. Same package and persistent certificate from v0.9.10. OTA beta is updated only after signed release, package check and byte-for-byte comparison.

## TV checks after installing
- Opening GTA live: at 1.7 seconds without a first frame, a visible loading label appears near lower-middle, then disappears when video starts.
- Open suggestions with Down: panel stays in bottom portion, play image visible above; scroll all rows with D-pad; Up returns to complete player controls without clipping.
- Exit to the Home with PiP: stable reserve appears quickly, left-side menu and scrolling still work. When video PiP closes, reserve disappears. The system may put PiP in a different spot.
- Try a second video, local recording / news live: no stale loading indicator after a successful start.
- Verify videos, subtitles, quality, 4K if available, live, history and internal updater.
- Record any inability to select rows/controls or any black screen without messages.

**Status:** code/design review and APK signing can be verified by GitHub; actual TV UX still requires direct user test. 0.9.10 stays published as fallback.
