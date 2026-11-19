#!/usr/bin/env python3
"""Patch upstream SmartTube source into an independent, reviewable Android TV preview.

Designed to run after a pinned upstream checkout with submodules, not to repack an APK.
Does NOT integrate Cobalt yet. Does NOT modify the Cobalt HX installation.
"""
from pathlib import Path
import argparse
import re
import sys

PACKAGE_ID = "br.com.hxnative.tvpreview"
LABEL = "HX TV Preview"
VERSION_NAME = "0.1.0"
VERSION_CODE = 2470
RELEASE_TAG = "hx-smarttube-native-v0.1.0"
REPO = "helbermoura2/cobalt-script"


def replace_exact(file: Path, old: str, new: str):
    text = file.read_text(encoding="utf-8")
    if text.count(old) != 1:
        raise RuntimeError(f"Required upstream marker not unique: {file}: {old!r}, found={text.count(old)}")
    file.write_text(text.replace(old, new), encoding="utf-8")
    print(f"PATCHED {file}")


def make_icons(root: Path):
    """Clearly distinguish from TizenTube/SmartTube. No third-party logos."""
    from PIL import Image, ImageDraw, ImageFont
    folder = root / "smarttubetv/src/main/res/mipmap-nodpi"
    font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
    for name, size, banner in (
        ("app_icon.png", (512, 512), False),
        ("app_logo.png", (512, 512), False),
        ("app_banner.png", (320, 180), True),
    ):
        im = Image.new("RGB", size, "#0b0c0f")
        d = ImageDraw.Draw(im)
        w, h = size
        for i in range(h):
            v = i / max(h, 1)
            c = (int(11+v*19), int(12+v*10), int(15+v*8))
            d.line((0, i, w, i), fill=c)
        inset=int(min(w,h)*0.10)
        d.rounded_rectangle((inset,inset,w-inset,h-inset),radius=int(min(w,h)*.16),outline="#c59a67",width=max(3,w//100))
        font=ImageFont.truetype(font_path, int(min(w,h)*(.27 if not banner else .25)))
        label="HX"
        bbox=d.textbbox((0,0),label,font=font)
        tw=bbox[2]-bbox[0];th=bbox[3]-bbox[1]
        d.text(((w-tw)/2,(h-th)/2-bbox[1]), label, fill="#f5d6a8", font=font)
        im.save(folder / name, "PNG", optimize=True)
        print("ICON", folder / name)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("upstream", type=Path)
    args=ap.parse_args()
    root=args.upstream.resolve()
    if not (root / "smarttubetv/build.gradle").exists():
        raise RuntimeError("Not a SmartTube source checkout")
    build=root / "smarttubetv/build.gradle"
    replace_exact(build, 'applicationId "org.smarttube.beta"', f'applicationId "{PACKAGE_ID}"')
    replace_exact(build, 'versionCode 2470', f'versionCode {VERSION_CODE}')
    replace_exact(build, 'versionName "32.80"', f'versionName "{VERSION_NAME}"')

    strings=root / "smarttubetv/src/main/res/values/strings.xml"
    replace_exact(strings, '<string translatable="false" name="app_name"><![CDATA[SmartTube]]></string>',
                  f'<string translatable="false" name="app_name"><![CDATA[{LABEL}]]></string>')
    replace_exact(strings, '<string translatable="false" name="browse_title"><![CDATA[SmartTube]]></string>',
                  f'<string translatable="false" name="browse_title"><![CDATA[{LABEL}]]></string>')

    # The preview is disabled at the ACTUAL native card component, not by pausing
    # a video behind an overlay. Playback on OK is not affected.
    card=(root/"smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/widgets/complexcardview/ComplexImageCardView.java")
    replace_exact(card, "        enableVideoPreview(selected);",
                  "        // HX: Autoplay video previews disabled on native TV cards.")

    # SmartTube's release manifest must never offer its ORIGINAL package as an
    # update for our package. The app's built-in Settings > About > Check for updates
    # now points only at our manifest and uses the existing installer workflow.
    url=f"https://raw.githubusercontent.com/{REPO}/smart-cobalt-hybrid/hybrid/updates/native.json"
    updates=root/"common/src/stbeta/res/values/update_urls.xml"
    updates.write_text('<?xml version="1.0" encoding="utf-8"?>\n'
                       '<resources>\n    <string-array name="update_urls">\n'
                       f'        <item>{url}</item>\n'
                       '    </string-array>\n</resources>\n',encoding="utf-8")
    # Disable upstream Google/Firebase tracking for our development preview.
    google=root/"smarttubetv/google-services.json"
    if google.exists():
        google.unlink()
        print("REMOVED upstream google-services.json")

    make_icons(root)
    # Output compatibility checks.
    b=build.read_text(encoding="utf-8")
    assert PACKAGE_ID in b and 'versionName "0.1.0"' in b
    assert "enableVideoPreview(selected);" not in card.read_text(encoding="utf-8")
    assert url in updates.read_text(encoding="utf-8")
    print("HX Native TV source patch validated; updater manifest =",url)


if __name__=="__main__":
    main()
