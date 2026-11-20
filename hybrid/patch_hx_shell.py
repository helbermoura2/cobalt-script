#!/usr/bin/env python3
"""HX TV 0.4 native shell: real Android Views, NOT a screen overlay.

Turns the home experience into a prominent cinematic first rail, an HX
navigation rail, hand-composed selected video tiles and a branded title area.
Preserves native playback, browsing, remote focus and marquee animations.
This module runs AFTER patch_smarttube.py and patch_hx_design.py.
"""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET

JAVA = Path("smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv")
RES = Path("smarttubetv/src/main/res")


def replace_once(path: Path, old: str, new: str):
    value = path.read_text(encoding="utf-8")
    num = value.count(old)
    if num != 1:
        raise RuntimeError(f"Source changed: {path}, marker {old[:100]!r}, count={num}")
    path.write_text(value.replace(old, new, 1), encoding="utf-8")
    print("HX SHELL PATCH", path)


def add_resources(path: Path, body: str):
    original = path.read_text(encoding="utf-8")
    if original.count("</resources>") != 1:
        raise RuntimeError(f"Bad resources: {path}")
    path.write_text(original.replace("</resources>", body + "\n</resources>", 1),
                    encoding="utf-8")
    ET.parse(path)


def write_xml(path: Path, content: str):
    if not path.parent.is_dir():
        raise RuntimeError(f"Missing Android resources folder: {path.parent}")
    path.write_text(content, encoding="utf-8")
    ET.parse(path)


def make_drawables(root: Path):
    draw = root / RES / "drawable"
    defs = {
        "hx_card_shell.xml": ('#FF0B0A0A', '#FF28211C', 1, 16),
        "hx_card_panel_idle.xml": ('#FF141413', '#FF24211E', 1, 0),
        "hx_card_panel_active.xml": ('#FF281F19', '#FFAB8359', 1, 0),
        "hx_card_outline.xml": ('#00000000', '#FFE4B579', 3, 16),
        "hx_settings_idle.xml": ('#FF191716', '#FF38302A', 1, 16),
        "hx_settings_active.xml": ('#FF30231B', '#FFC99B64', 2, 16),
        "hx_nav_idle.xml": ('#00140F0C', '#00000000', 0, 13),
        "hx_nav_active.xml": ('#FF2D211A', '#FFC99B64', 1, 13),
    }
    for name, (bg, stroke, width, radius) in defs.items():
        xml = (f'<?xml version="1.0" encoding="utf-8"?>\n'
               f'<shape xmlns:android="http://schemas.android.com/apk/res/android" '
               f'android:shape="rectangle">\n'
               f'  <solid android:color="{bg}"/>\n'
               f'  <corners android:radius="{radius}dp"/>\n'
               f'  <stroke android:color="{stroke}" android:width="{width}dp"/>\n'
               f'</shape>\n')
        write_xml(draw / name, xml)


def patch_card_presenter(root: Path):
    source = root / JAVA / "presenter/VideoCardPresenter.java"
    replace_once(source,
                 "        cardView.setBackgroundColor(mDefaultBackgroundColor); // background is temporarily visible during animations",
                 """        // HX: separate thumbnail, dark info plate and bronze selection frame.
        // This is applied to the native ImageCardView, not an overlay.
        cardView.setBackgroundResource(R.drawable.hx_card_shell);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            cardView.setClipToOutline(true);
        }""")
    replace_once(source,
                 "        int backgroundColor = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;",
                 """        int backgroundColor = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;
        view.setBackgroundResource(R.drawable.hx_card_shell);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            view.setElevation(selected ? 10f : 2f);
        }
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            view.setForeground(selected ?
                    ContextCompat.getDrawable(view.getContext(), R.drawable.hx_card_outline) : null);
        }""")
    replace_once(source,
                 "            infoField.setBackgroundColor(backgroundColor);",
                 """            infoField.setBackgroundResource(selected ?
                    R.drawable.hx_card_panel_active : R.drawable.hx_card_panel_idle);""")
    replace_once(source,
                 "            titleText.setTextColor(textColor);",
                 """            titleText.setTextColor(ContextCompat.getColor(view.getContext(),
                    selected ? R.color.hx_text : R.color.hx_text));
            titleText.setTypeface(android.graphics.Typeface.create(
                    "sans-serif-medium", android.graphics.Typeface.NORMAL));""")
    replace_once(source,
                 "            contentText.setTextColor(textColor);",
                 """            contentText.setTextColor(ContextCompat.getColor(view.getContext(),
                    selected ? R.color.hx_bronze_focus : R.color.hx_subtitle));""")
    # Keep the exact marquee code and its 1000ms delay untouched.


def patch_first_row(root: Path):
    card = root / JAVA / "presenter/HxSpotlightCardPresenter.java"
    card.write_text("""package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.util.Pair;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;

/**
 * HX cinematic spotlight rail. Uses the exact native player item and click
 * handlers of VideoCardPresenter, but gives the leading row a wider 16:9 stage.
 */
public final class HxSpotlightCardPresenter extends VideoCardPresenter {
    @Override
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(
                context, R.dimen.hx_spotlight_width, R.dimen.hx_spotlight_height,
                MainUIData.instance(context).getVideoGridScale());
    }
}
""", encoding="utf-8")

    fragment = root / JAVA / "ui/browse/video/MultipleRowsFragment.java"
    replace_once(fragment,
                 "import com.liskovsoft.smartyoutubetv2.tv.presenter.VideoCardPresenter;",
                 "import com.liskovsoft.smartyoutubetv2.tv.presenter.VideoCardPresenter;\n"
                 "import com.liskovsoft.smartyoutubetv2.tv.presenter.HxSpotlightCardPresenter;")
    replace_once(fragment,
                 "    private VideoCardPresenter mCardPresenter;",
                 "    private VideoCardPresenter mCardPresenter;\n"
                 "    private HxSpotlightCardPresenter mSpotlightPresenter;")
    replace_once(fragment,
                 "        mCardPresenter = new VideoCardPresenter();",
                 "        mCardPresenter = new VideoCardPresenter();\n"
                 "        mSpotlightPresenter = new HxSpotlightCardPresenter();")
    replace_once(fragment,
                 "        mCardPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());",
                 "        mCardPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());\n"
                 "        mSpotlightPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());")
    replace_once(fragment,
                 "            VideoGroupObjectAdapter videoGroupAdapter = new VideoGroupObjectAdapter(group, group.isShorts() ? mShortsPresenter : mCardPresenter);",
                 """            // HX: first rail gets wide hero cards; remaining rails are compact.
            // Both use the SAME Video model and native player/click callbacks.
            boolean isSpotlight = mRowsAdapter.size() == 0
                    && mChannelHeaderCallback == null && !group.isShorts();
            VideoGroupObjectAdapter videoGroupAdapter = new VideoGroupObjectAdapter(group,
                    group.isShorts() ? mShortsPresenter :
                    isSpotlight ? mSpotlightPresenter : mCardPresenter);""")
    add_resources(root / RES / "values/dimens.xml",
                  '<dimen name="hx_spotlight_width">324dp</dimen>\n'
                  '    <dimen name="hx_spotlight_height">182dp</dimen>')


def patch_nav(root: Path):
    layout = root / RES / "layout/icon_header_item.xml"
    write_xml(layout, """<?xml version="1.0" encoding="utf-8"?>
<!-- HX TV native navigation rail. Retains upstream IDs and D-pad focus. -->
<androidx.leanback.widget.NonOverlappingLinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:minHeight="54dp"
    android:paddingStart="13dp"
    android:paddingEnd="16dp"
    android:paddingTop="11dp"
    android:paddingBottom="11dp"
    android:background="@drawable/hx_nav_idle">

    <ImageView
        android:id="@+id/header_icon"
        android:layout_width="27dp"
        android:layout_height="27dp"
        android:scaleType="fitCenter"
        android:contentDescription="@string/header_icon"/>

    <com.liskovsoft.smartyoutubetv2.tv.ui.widgets.marqueetextviewcompat.HeaderMarqueeTextViewCompat
        android:id="@+id/header_label"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="14dp"
        android:singleLine="true"
        android:maxWidth="172dp"
        android:ellipsize="marquee"
        android:scrollHorizontally="true"
        android:marqueeRepeatLimit="marquee_forever"
        android:textSize="17sp"
        android:fontFamily="sans-serif-medium"
        android:textColor="@color/hx_text"/>
</androidx.leanback.widget.NonOverlappingLinearLayout>
""")
    java = root / JAVA / "presenter/IconHeaderItemPresenter.java"
    replace_once(java,
                 """    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        holder.view.setAlpha(mUnselectedAlpha + holder.getSelectLevel() *
                (1.0f - mUnselectedAlpha));
    }""",
                 """    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        float level = holder.getSelectLevel();
        holder.view.setAlpha(mUnselectedAlpha + level * (1.0f - mUnselectedAlpha));
        // Bronze tab is part of the native headers fragment; no mask.
        boolean active = level > 0.55f;
        holder.view.setBackgroundResource(active ?
                R.drawable.hx_nav_active : R.drawable.hx_nav_idle);
        TextView label = holder.view.findViewById(R.id.header_label);
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(holder.view.getContext(),
                    active ? R.color.hx_bronze_focus : R.color.hx_text));
        }
    }""")


def patch_brand_header(root: Path):
    f = root / RES / "layout/lb_title_view_logo.xml"
    old = '    <!-- Global clock -->'
    new = """    <!-- HX TV permanent native brand beside the existing clock. -->
    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_vertical"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="10dp"
        android:paddingEnd="16dp">
        <View
            android:layout_width="3dp"
            android:layout_height="36dp"
            android:layout_marginEnd="12dp"
            android:background="@color/hx_bronze"/>
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="vertical">
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="HX TV"
                android:textSize="25sp"
                android:fontFamily="sans-serif-black"
                android:letterSpacing="0.13"
                android:textColor="@color/hx_text"/>
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="SEU UNIVERSO"
                android:textSize="10sp"
                android:letterSpacing="0.23"
                android:fontFamily="sans-serif-medium"
                android:textColor="@color/hx_bronze"/>
        </LinearLayout>
    </LinearLayout>

    <!-- Global clock -->"""
    replace_once(f, old, new)
    ET.parse(f)


def patch_settings(root: Path):
    source = root / JAVA / "presenter/SettingsCardPresenter.java"
    replace_once(source,
                 "        container.setBackgroundColor(mDefaultBackgroundColor);",
                 "        container.setBackgroundResource(R.drawable.hx_settings_idle);")
    replace_once(source,
                 "        textView.setBackgroundColor(mDefaultBackgroundColor);",
                 "        textView.setBackgroundColor(android.graphics.Color.TRANSPARENT);")
    replace_once(source,
                 "            textView.setBackgroundColor(backgroundColor);\n"
                 "            textView.setTextColor(textColor);",
                 "            container.setBackgroundResource(hasFocus ?\n"
                 "                    R.drawable.hx_settings_active : R.drawable.hx_settings_idle);\n"
                 "            textView.setBackgroundColor(android.graphics.Color.TRANSPARENT);\n"
                 "            textView.setTextColor(ContextCompat.getColor(context,\n"
                 "                    hasFocus ? R.color.hx_text : R.color.hx_subtitle));")
    # Existing enabled marquee behaviour remains as-is.


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("upstream", type=Path)
    args = parser.parse_args()
    root = args.upstream.resolve()
    if not (root / "smarttubetv/build.gradle").exists():
        raise RuntimeError("Run with pinned SmartTube upstream root")
    add_resources(root / RES / "values/colors.xml",
                  '<color name="hx_subtitle">#FFB6AEA6</color>')
    make_drawables(root)
    patch_card_presenter(root)
    patch_first_row(root)
    patch_nav(root)
    patch_brand_header(root)
    patch_settings(root)
    for path in (
        root / RES / "layout/icon_header_item.xml",
        root / RES / "layout/lb_title_view_logo.xml",
        root / RES / "values/colors.xml",
        root / RES / "values/dimens.xml",
    ):
        ET.parse(path)
    print("HX SHELL READY: cinematic hero rail, native selected cards, rail, title and settings")
    print("HX MARQUEE/PREVIEW/PLAYER: unchanged from previous patches")


if __name__ == "__main__":
    main()
