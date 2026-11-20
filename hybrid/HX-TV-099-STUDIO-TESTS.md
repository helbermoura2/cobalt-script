# HX TV 0.9.9 Studio — release candidate

## Why this version exists
The 0.9.8 video review identified six areas: the miniplayer covering the upper-right, crowded Home hierarchy, weak live discovery organization, overly dominant player controls, inconsistent visual hierarchy and limited navigation/long-use verification. The 0.9.7 retractable stories remain rejected.

## Implementation (Android native)
1. **PiP & safe area** — The playback activity specifies 16:9 Picture-in-Picture, while the Home header keeps the upper right free of active controls. On Android TV the OS, not HX, chooses the final floating window position: repositioning arbitrary PiP windows is not promised.
2. **Home** — Smaller editorial hero (142dp) with title left, loading/debounce of artwork, native navigation rail with source icon drawables, consistent dark graphite and pearl tones. Recommendations are deduplicated across Home rows by video ID, while live discovery remains available separately.
3. **Live hub** — Actual `SearchOptions.FEATURE_LIVE | TYPE_VIDEO` requests, verify video is live and not ended/upcoming, eight topical filters operate inside the HX interface and provide counts plus manual refresh. Changes do not rebuild a focused row. Results depend on real availability.
4. **Player** — Native action IDs preserved. Buttons use translucent graphite, subtle focus, less intrusive controls backdrop, 168×95dp 16:9 playback suggestions; secondary codec/time labels are less visually dominant. No secondary controls have been deleted.
5. **Typography, animation, iconography** — Menu uses native category icons where resource IDs exist, otherwise falls back to glyphs. Text styles and vertical spacing standardized. Short reversible focus transitions avoid reflow of card widths.
6. **Performance & tests** — Render the first 20 cards of a shelf and progressively add 12 while navigating. Guard stale live search callbacks, cancel them on section switch, debounce hero loads; run `hybrid/qa_hx_099.py` plus Android build/signature checks.

## Mandatory physical acceptance on TCL P7K
- Launch, menu icon navigation, toggle rail, select the first video, navigate every row, return from player, History and search.
- PiP while returning to Home: no label or button must become inaccessible; 16:9 aspect ratio when supported. Note PiP position is chosen by the OS.
- Home with many results, no duplicate video IDs across recommendations, live row still distinct.
- Enter AO VIVO: all eight filters, Todos and Atualizar. Search results must be live, not recorded; empty category must show useful text. Reload while a video is focused in a row.
- Watch recorded and live video; test 4K/60 when available, subtitles, quality, playback speed, recommendations and all original player actions.
- Confirm no jump on fast D-pad navigation, no crashed/leaked activity after entering and exiting playback repeatedly.
- Verify in-app Update installs over signed 0.9.8 without uninstalling; compare certificate and package ID.
- Confirm visual quality at 720p and 1080p and long-running playback. The CI build is NOT a substitute for this TV test.

## Delivery
Current 0.9.8 beta stays published. The 0.9.9 APK is published and its in-app update manifest activated **only** after Gradle succeeds, APK signer v2/v3 matches the known persistent certificate, and the downloaded release asset matches the local build byte for byte.
