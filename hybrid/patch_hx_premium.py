#!/usr/bin/env python3
"""HX TV 0.6: compact native leanback layout and restrained premium focus.

Runs AFTER patch_smarttube.py, patch_hx_design.py, and patch_hx_shell.py.
Removes the bridge-SmartTube badge at the source instead of hiding an overlay.
All changes are Android layouts/drawables/classes. Keeps playback and marquee.
"""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET

TV = Path("smarttubetv/src/main")
JAVA = TV / "java/com/liskovsoft/smartyoutubetv2/tv"
RES = TV / "res"


def replace_once(file: Path, old: str, new: str):
    current = file.read_text(encoding="utf-8")
    n = current.count(old)
    if n != 1:
        raise RuntimeError(f"Source marker mismatch: {file}, count={n}, marker={old!r}")
    file.write_text(current.replace(old, new, 1), encoding="utf-8")
    print("HX PREMIUM PATCH", file)


def drawable(file: Path, fill: str, border: str, thickness: float, radius: float):
    # A native shape drawable, not a bitmap, webview or overlaid screen.
    file.write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="{fill}"/>
    <corners android:radius="{radius}dp"/>
    <stroke android:color="{border}" android:width="{thickness}dp"/>
</shape>
''', encoding="utf-8")
    ET.parse(file)


def remove_foreign_badge(root: Path):
    source = root / JAVA / "ui/browse/BrowseFragment.java"
    replace_once(
        source,
        "        setBadgeDrawable(bridgeIcon != null ? bridgeIcon : appLogoRes > 0 ? ContextCompat.getDrawable(getContext(), appLogoRes) : null);",
        """        // HX TV branding lives in the actual native title layout.
        // The bridge package points at the SmartTube beta icon even after
        // resources were replaced, so never request or display that badge.
        setBadgeDrawable(null);""",
    )
    assert "setBadgeDrawable(null);" in source.read_text(encoding="utf-8")


def compact_menu(root: Path):
    dims = root / RES / "values/dimens.xml"
    replace_once(dims,
                 '<dimen name="lb_browse_rows_margin_start">265dp</dimen>',
                 '<dimen name="lb_browse_rows_margin_start">210dp</dimen>')
    replace_once(dims,
                 '<dimen name="lb_browse_rows_margin_top">130dp</dimen>',
                 '<dimen name="lb_browse_rows_margin_top">116dp</dimen>')
    replace_once(dims,
                 '<dimen name="browse_headers_vertical_spacing">10dp</dimen>',
                 '<dimen name="browse_headers_vertical_spacing">6dp</dimen>')

    # This XML is the actual Leanback row header inflated by the app.
    # Shrink both the item and the label, preserving IDs, click and D-pad.
    layout = root / RES / "layout/icon_header_item.xml"
    for old,new in (
        ('android:minHeight="54dp"', 'android:minHeight="48dp"'),
        ('android:paddingStart="13dp"', 'android:paddingStart="10dp"'),
        ('android:paddingEnd="16dp"', 'android:paddingEnd="6dp"'),
        ('android:paddingTop="11dp"', 'android:paddingTop="8dp"'),
        ('android:paddingBottom="11dp"', 'android:paddingBottom="8dp"'),
        ('android:layout_width="27dp"', 'android:layout_width="24dp"'),
        ('android:layout_height="27dp"', 'android:layout_height="24dp"'),
        ('android:layout_marginStart="14dp"', 'android:layout_marginStart="11dp"'),
        ('android:maxWidth="172dp"', 'android:maxWidth="137dp"'),
        ('android:textSize="17sp"', 'android:textSize="15sp"'),
    ):
        replace_once(layout, old, new)
    ET.parse(layout)


def restrained_video_cards(root: Path):
    shapes = root / RES / "drawable"
    drawable(shapes / "hx_card_shell.xml", "#FF0D0D0E", "#FF201B17", 1, 13)
    drawable(shapes / "hx_card_outline.xml", "#00000000", "#B0C99B64", 1, 13)
    drawable(shapes / "hx_card_panel_idle.xml", "#FF151518", "#00000000", 0, 0)
    drawable(shapes / "hx_card_panel_active.xml", "#FF262228", "#00000000", 0, 0)
    drawable(shapes / "hx_nav_idle.xml", "#00000000", "#00000000", 0, 10)
    drawable(shapes / "hx_nav_active.xml", "#FF241D17", "#8CC99B64", 1, 10)
    drawable(shapes / "hx_settings_idle.xml", "#FF181819", "#44332E29", 1, 12)
    drawable(shapes / "hx_settings_active.xml", "#FF292320", "#A9C99B64", 1, 12)

    # FocusHighlight.ZOOM_FACTOR_XSMALL is supported by the pinned Leanback
    # implementation; keep focus animation but avoid huge cards on TCL P7K.
    view = root / JAVA / "util/ViewUtil.java"
    replace_once(view,
        "public static final int FOCUS_ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;",
        "public static final int FOCUS_ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_XSMALL;")

    card = root / JAVA / "presenter/VideoCardPresenter.java"
    replace_once(card, "view.setElevation(selected ? 10f : 2f);",
                 "view.setElevation(selected ? 5f : 1f);")

    dims = root / RES / "values/dimens.xml"
    for old,new in (
        ('<dimen name="hx_spotlight_width">324dp</dimen>',
         '<dimen name="hx_spotlight_width">286dp</dimen>'),
        ('<dimen name="hx_spotlight_height">182dp</dimen>',
         '<dimen name="hx_spotlight_height">161dp</dimen>'),
        ('<dimen name="lb_basic_card_info_padding">6dp</dimen>',
         '<dimen name="lb_basic_card_info_padding">5dp</dimen>'),
        ('<dimen name="lb_basic_card_title_text_size">18sp</dimen>',
         '<dimen name="lb_basic_card_title_text_size">16sp</dimen>'),
        ('<dimen name="lb_basic_card_content_text_size">13sp</dimen>',
         '<dimen name="lb_basic_card_content_text_size">12sp</dimen>'),
    ):
        replace_once(dims,old,new)


def clean_header(root: Path):
    # Existing HX native title already renders HX TV / SEU UNIVERSO and clock.
    # Keep it while slightly reducing its footprint; top-right SmartTube beta
    # badge is separately removed via BrowseFragment.updateBadge().
    file = root / RES / "layout/lb_title_view_logo.xml"
    replace_once(file, 'android:textSize="25sp"', 'android:textSize="22sp"')
    replace_once(file, 'android:textSize="10sp"', 'android:textSize="9sp"')
    replace_once(file, 'android:layout_height="36dp"', 'android:layout_height="30dp"')
    replace_once(file, 'android:paddingEnd="16dp"', 'android:paddingEnd="10dp"')
    ET.parse(file)


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("upstream",type=Path)
    args=parser.parse_args()
    root=args.upstream.resolve()
    if not (root/"smarttubetv/build.gradle").exists():
        raise RuntimeError("Pinned SmartTube checkout missing")
    remove_foreign_badge(root)
    compact_menu(root)
    restrained_video_cards(root)
    clean_header(root)
    ET.parse(root/RES/"values/dimens.xml")
    for file in (root/RES/"layout/icon_header_item.xml",
                 root/RES/"layout/lb_title_view_logo.xml"):
        ET.parse(file)
    assert "ZOOM_FACTOR_XSMALL" in (root/JAVA/"util/ViewUtil.java").read_text()
    print("HX TV PREMIUM OK: old bridge badge removed, 210dp menu, subtle card focus")
    print("HX TV INPUT/PLAYER/MARQUEE: preserved unchanged")


if __name__=="__main__":
    main()
