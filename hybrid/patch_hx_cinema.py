#!/usr/bin/env python3
"""HX TV 0.7 — true cinematic feature rail using native Android views.

This replaces the previous first-row 'large SmartTube card' with a dedicated
native Presenter.  Focus, click events, long press, and Video models continue
through Leanback/MultipleRowsFragment. No WebView, video mask, or autoplay.
"""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET

TV = Path("smarttubetv/src/main")
SOURCE = TV / "java/com/liskovsoft/smartyoutubetv2/tv"
RES = TV / "res"

HERO_JAVA = r"""package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;

/**
 * HX TV's first recommendation rail: feature artwork with real overlay
 * typography INSIDE the native card (not an overlay over the app).
 * Clicks and D-pad navigation continue to use the exact same Video model.
 */
public final class HxSpotlightCardPresenter extends LongClickPresenter {
    private static final int BG = 0xFF111111;
    private static final int GOLD = 0xFFD2AA74;
    private static final int WHITE = 0xFFF7F4F0;
    private static final int MUTED = 0xFFE0D4C7;

    static int dp(Context c, float units) {
        return Math.round(c.getResources().getDisplayMetrics().density * units);
    }

    private static GradientDrawable shape(int fill, int strokeWidth, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        if (strokeWidth > 0) {
            d.setStroke(strokeWidth, stroke);
        }
        return d;
    }

    static class HeroCard extends FrameLayout {
        final ImageView art;
        final TextView title;
        final TextView meta;
        final TextView kicker;
        final TextView live;
        final ProgressBar watch;
        final int cardW;
        final int cardH;

        HeroCard(Context context) {
            super(context);
            float scale = MainUIData.instance(context).getVideoGridScale();
            // Keep measurements controlled on small screens and TV resolutions.
            float clamped = Math.max(0.85f, Math.min(scale, 1.20f));
            cardW = Math.round(dp(context, 436) * clamped);
            cardH = Math.round(dp(context, 250) * clamped);
            setLayoutParams(new ViewGroup.LayoutParams(cardW, cardH));
            setFocusable(true);
            setFocusableInTouchMode(true);
            setClipToPadding(false);
            setBackground(shape(BG, dp(context, 1), 0xFF302820, dp(context, 16)));

            art = new ImageView(context);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            FrameLayout.LayoutParams artworkParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            int inner = dp(context, 2);
            artworkParams.setMargins(inner, inner, inner, inner);
            addView(art, artworkParams);

            View bottomShade = new View(context);
            bottomShade.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{0xFA050505, 0xE3080809, 0x90070809, 0x00070809}));
            FrameLayout.LayoutParams shadeParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 150), Gravity.BOTTOM);
            addView(bottomShade, shadeParams);

            LinearLayout description = new LinearLayout(context);
            description.setOrientation(LinearLayout.VERTICAL);
            description.setGravity(Gravity.BOTTOM | Gravity.START);
            description.setPadding(dp(context, 22), 0, dp(context, 24), dp(context, 18));
            FrameLayout.LayoutParams descriptionParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.START);
            addView(description, descriptionParams);

            kicker = new TextView(context);
            kicker.setText("HX TV  /  EM DESTAQUE");
            kicker.setAllCaps(true);
            kicker.setTextColor(GOLD);
            kicker.setTextSize(10);
            kicker.setLetterSpacing(0.17f);
            kicker.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            kicker.setPadding(0, 0, 0, dp(context, 5));
            description.addView(kicker);

            title = new TextView(context);
            title.setSingleLine(true);
            title.setTextColor(WHITE);
            title.setTextSize(23);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            title.setEllipsize(TextUtils.TruncateAt.END);
            description.addView(title);

            meta = new TextView(context);
            meta.setSingleLine(true);
            meta.setTextSize(13);
            meta.setTextColor(MUTED);
            meta.setEllipsize(TextUtils.TruncateAt.END);
            meta.setPadding(0, dp(context, 7), 0, 0);
            description.addView(meta);

            live = new TextView(context);
            live.setText("● AO VIVO");
            live.setTextColor(WHITE);
            live.setTextSize(11);
            live.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            live.setPadding(dp(context, 10), dp(context, 5),
                    dp(context, 10), dp(context, 5));
            live.setBackground(shape(0xDAA61E21, 0, 0, dp(context, 8)));
            FrameLayout.LayoutParams liveParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END);
            liveParams.setMargins(0, dp(context, 17), dp(context, 17), 0);
            addView(live, liveParams);
            live.setVisibility(View.GONE);

            watch = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            watch.setMax(100);
            if (Build.VERSION.SDK_INT >= 21) {
                watch.setProgressTintList(ColorStateList.valueOf(GOLD));
                watch.setProgressBackgroundTintList(ColorStateList.valueOf(0x66666666));
            }
            FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 3), Gravity.BOTTOM);
            progressParams.setMargins(dp(context, 5), 0, dp(context, 5), dp(context, 3));
            addView(watch, progressParams);
            watch.setVisibility(View.GONE);
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            setBackground(shape(BG, dp(getContext(), selected ? 2 : 1),
                    selected ? GOLD : 0xFF302820, dp(getContext(), 16)));
            if (Build.VERSION.SDK_INT >= 21) {
                setElevation(dp(getContext(), selected ? 7 : 1));
            }
            // Retain the user's marquee for long featured titles.
            title.setEllipsize(selected ? TextUtils.TruncateAt.MARQUEE
                    : TextUtils.TruncateAt.END);
            title.setMarqueeRepeatLimit(-1);
            title.setSelected(selected);
            meta.setEllipsize(selected ? TextUtils.TruncateAt.MARQUEE
                    : TextUtils.TruncateAt.END);
            meta.setMarqueeRepeatLimit(-1);
            meta.setSelected(selected);
        }
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new Presenter.ViewHolder(new HeroCard(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);
        Video video = (Video) item;
        HeroCard card = (HeroCard) viewHolder.view;
        Context context = card.getContext();
        card.title.setText(video.getTitle());
        card.meta.setText(video.getSecondTitle());
        card.live.setVisibility(video.isLive ? View.VISIBLE : View.GONE);
        int percent = Math.max(0, Math.min(100, Math.round(video.percentWatched)));
        card.watch.setProgress(percent);
        card.watch.setVisibility(percent > 0 ? View.VISIBLE : View.GONE);
        if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            return;
        }
        Glide.with(context)
                .load(ClickbaitRemover.updateThumbnail(
                        video, MainUIData.instance(context).getThumbQuality()))
                .override(card.cardW, card.cardH)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.card_placeholder)
                .into(card.art);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        super.onUnbindViewHolder(viewHolder);
        HeroCard card = (HeroCard) viewHolder.view;
        Glide.with(card.getContext().getApplicationContext()).clear(card.art);
        card.art.setImageDrawable(null);
        card.title.setText(null);
        card.meta.setText(null);
    }
}
""".replace('import android.graphics.Color;\n','')

def replace_exact(path: Path, old: str, new: str):
    text=path.read_text(encoding="utf-8")
    n=text.count(old)
    if n!=1: raise RuntimeError(f"Expected one marker in {path}: {old!r}, found {n}")
    path.write_text(text.replace(old,new,1),encoding="utf-8")

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("upstream",type=Path)
    root=parser.parse_args().upstream.resolve()
    path=root/SOURCE/"presenter/HxSpotlightCardPresenter.java"
    if not path.exists():
        raise RuntimeError("Run after patch_hx_shell.py")
    if "class HxSpotlightCardPresenter extends VideoCardPresenter" not in path.read_text():
        raise RuntimeError("Existing first-row presenter has changed")
    path.write_text(HERO_JAVA,encoding="utf-8")
    # The first row now has a distinct 436x250 cinematic native card; other
    # rows continue using VideoCardPresenter and original data sources.
    rows=root/SOURCE/"ui/browse/video/MultipleRowsFragment.java"
    replace_exact(rows,
        "            HeaderItem rowHeader = new HeaderItem(group.getTitle());",
        '''            // Editorial label for the native feature rail; only the first
            // ordinary video row is renamed, other categories stay unchanged.
            boolean hxFirstRail = mRowsAdapter.size() == 0 &&
                    mChannelHeaderCallback == null && !group.isShorts();
            HeaderItem rowHeader = new HeaderItem(
                    hxFirstRail ? "EM DESTAQUE" : group.getTitle());''')
    # Do not modify the long-press/click handler: the native HxSpotlight
    # Presenter inherits LongClickPresenter and passes the original Video.
    assert "HxSpotlightCardPresenter" in rows.read_text(encoding="utf-8")
    assert "super.onBindViewHolder(viewHolder, item);" in HERO_JAVA
    assert "android.webkit" not in HERO_JAVA
    print("HX CINEMA: native full-bleed feature tiles + gradient typography + live/watch badges")
    print("HX CINEMA: player, D-pad click/long press, content and marquee retained")

if __name__=="__main__":
    main()
