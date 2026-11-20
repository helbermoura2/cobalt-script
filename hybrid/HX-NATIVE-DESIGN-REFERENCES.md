# HX TV — Design references and native-only interface criteria
Date: 2026-11-20
Status: design / architecture plan, not yet an APK. Do not publish this document as a completed UI implementation.

## Core decision
Stop the incremental visual patch loop on SmartTube's Leanback BrowseFragment.
Build an independent HX TV home and navigation shell with Android-native views.
Reuse the existing SmartTube video service, account/authentication logic, history
and playback presenter rather than rendering the original SmartTube UI behind ours.
No WebView, masks, screen overlays, double browse screens, or auto-playing previews.

## Sources checked
- **Taste Skill** https://www.tasteskill.dev — editorial layouts, deliberate
  hierarchy, QA at multiple viewport sizes. Inspiration only: its automated
  web-browser inspector and npm skill aren't Android tools.
- **Transitions.dev** https://transitions.dev — card resize, panel reveal,
  skeleton-to-content crossfade, soft text swap, focus interaction. Implement
  native equivalents with Android Animator/Transition APIs.
- **React Bits** https://reactbits.dev — curated text, micro-interaction,
  and background ideas. React/CSS components cannot be imported into an Android
  TV native activity; reinterpret rather than grafting React.

## New HX home screen, not a Leanback reskin
Screen under one native Android Activity/Fragment controlled by the D-pad:
1. Header and category selector with HX-specific brand, search, account,
   live, games, subscriptions and settings.
2. Editorial hero area: focused recommended video artwork aligned right;
   real text/metadata and actions on a dark reserved left column. Do not place
   text over faces or detailed artwork. Avoid duplicating thumbnail text.
3. Compact horizontal recommendation shelf with focus that updates hero
   content but does NOT auto-play. Rows below use a cohesive original HX tile.
4. App menu can be revealed or collapsed without permanently reserving 265dp
   for a Leanback headers pane. Focus must always be recoverable by Left/Back.
5. The SmartTube playback activity/service is the only video player. Reuse
   existing click/long-click and voice input where possible.
6. Persistent user account, history, recommended feeds and session state.

## Motion budget / suggested native mappings
- 160–200 ms focus scale to max 1.035, subtle elevation, bronze edge;
  no large beige focus panels or hover-only behavior.
- 180–240 ms hero artwork crossfade when the selected item changes; fade
  text separately. Never start a video preview on focus.
- 200–260 ms menu slide using translationX and alpha.
- 150–200 ms skeleton crossfade to real thumbnails when data arrives.
- Accessibility: honor reduce-motion setting when available, avoid animated
  flashing gradients, keep focus outline perceivable from TV distance.
- Stay compatible with the user's existing marquee behavior for truncated
  labels.

## Acceptance checks BEFORE describing an APK as release-ready
- Verify at 1280x720 and 1920x1080, menu both open and closed:
  zero off-screen/cropped text or buttons. In the 0.8 photos the hero title
  escaped the top edge and overlapped HX TV header — this must not recur.
- Confirm distinct HX home, *not* two oversized SmartTube thumbnails.
- Confirm DPAD Left/Right/Up/Down, OK, long-OK and Back across all panes.
- Check search, login/account, subscriptions, history, live and video playback.
- Confirm no duplicate SmartTube badge, no stale overlay, and correct title.
- Verify RAM/performance on TCL P7K; images may be lower resolution while
  animations run, no web render layer.
- Run Android build, verify v1/v2/v3 permanent signing and monotonic version.
- No update channel change until screenshot/TV test of the independent shell
  and playback. Existing signed 0.8 stays available in meantime.
- Do not claim 'finished' merely because Gradle succeeded: build success
  is not visual, D-pad, or playback verification.

## Scope warning
These references improve design direction, not Android compatibility out
of the box. Do not add npm/React, web overlays, or third-party paid libraries
to the native project. Preserve free and reproducible GitHub build workflow.
