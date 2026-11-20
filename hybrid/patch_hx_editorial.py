#!/usr/bin/env python3
"""HX TV 0.8: new editorial Home layout (not a restyled SmartTube card).

One full-width native Android featured panel above a compact selectable
recommendation strip. All other categories retain known-good playback,
remote navigation, and YouTube data source.
"""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET
BASE=Path("smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv")
RES=Path("smarttubetv/src/main/res")
PRES=BASE/"presenter"
ROWS=BASE/"ui/browse/video/MultipleRowsFragment.java"

def replace_one(p, a, b):
    s=p.read_text(encoding="utf-8")
    n=s.count(a)
    if n!=1: raise RuntimeError(f"Missing or duplicate source marker in {p}: {a!r} ({n})")
    p.write_text(s.replace(a,b,1),encoding="utf-8")

FEATURE_ROW=r'''package com.liskovsoft.smartyoutubetv2.tv.presenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ObjectAdapter;
/** HX home only: editorial hero plus real selectable miniatures. */
public final class HxFeaturedRow extends ListRow {
    public HxFeaturedRow(HeaderItem header, ObjectAdapter adapter) {
        super(header, adapter);
    }
}
'''

MINI=r'''package com.liskovsoft.smartyoutubetv2.tv.presenter;
import android.content.Context;
import android.util.Pair;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
/** Short horizontal discover strip under the HX featured editorial panel. */
public final class HxEditorialMiniCardPresenter extends VideoCardPresenter {
    @Override protected Pair<Integer,Integer> getCardDimensPx(Context c) {
        return GridFragmentHelper.getCardDimensPx(c,
            R.dimen.hx_mini_card_width, R.dimen.hx_mini_card_height,
            MainUIData.instance(c).getVideoGridScale());
    }
}
'''

PRESENTER=r'''package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.app.Activity;
import android.content.Context;
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
import android.widget.TextView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ListRowView;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.lang.ref.WeakReference;

/**
 * HX Editorial home: TWO distinct native layout levels.
 * Above: an uncluttered, full-width, 220dp feature scene with readable left
 * title and artwork extending across the right side.
 * Below: a compact native Leanback D-pad strip, each item opens the real video.
 * The scene is a child of the ListRowView, NOT a screen-wide overlay.
 */
public final class HxEditorialRowPresenter extends CustomListRowPresenter {
    private WeakReference<FeaturedScene> lastScene = new WeakReference<>(null);
    private WeakReference<ListRow> currentRow = new WeakReference<>(null);

    static int dp(Context c, int value) {
        return (int)(value*c.getResources().getDisplayMetrics().density + 0.5f);
    }

    static class FeaturedScene extends FrameLayout {
        ImageView art;
        TextView title;
        TextView meta;
        TextView live;
        TextView play;
        Video currentVideo;

        FeaturedScene(Context c) {
            super(c);
            setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 224)));
            setBackgroundColor(0xFF09090A);
            setClipChildren(true);
            setClipToPadding(true);
            if (Build.VERSION.SDK_INT >= 21) setElevation(dp(c, 1));

            art = new ImageView(c);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            addView(art, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            View shade = new View(c);
            shade.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF08080A, 0xFF0A0A0C, 0xE60A0A0C,
                        0x8A0A0A0C, 0x080A0A0C}));
            addView(shade, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            LinearLayout content = new LinearLayout(c);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            content.setPadding(dp(c, 19), dp(c, 12), dp(c, 10), dp(c, 12));
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                    dp(c, 410), ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER_VERTICAL | Gravity.LEFT);
            addView(content, cp);

            TextView eyebrow = new TextView(c);
            eyebrow.setText("HX ORIGINAL  /  EM DESTAQUE");
            eyebrow.setTextColor(0xFFD1A678);
            eyebrow.setTextSize(10);
            eyebrow.setLetterSpacing(0.18f);
            eyebrow.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            content.addView(eyebrow);

            title = new TextView(c);
            title.setTextColor(0xFFF9F5F0);
            title.setTextSize(28);
            title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            title.setMaxLines(3);
            title.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = dp(c, 10);
            content.addView(title, titleParams);

            meta = new TextView(c);
            meta.setTextColor(0xFFD7CCC0);
            meta.setTextSize(13);
            meta.setMaxLines(2);
            meta.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mp.topMargin=dp(c, 10);
            content.addView(meta, mp);

            LinearLayout actions = new LinearLayout(c);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ap.topMargin=dp(c, 15);
            content.addView(actions, ap);

            play = new TextView(c);
            play.setText("▶   ASSISTIR   ·   OK");
            play.setTextColor(0xFF121110);
            play.setTextSize(12);
            play.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
            play.setPadding(dp(c,13),dp(c,8),dp(c,13),dp(c,8));
            GradientDrawable bronze = new GradientDrawable();
            bronze.setCornerRadius(dp(c,7));
            bronze.setColor(0xFFE0BB88);
            play.setBackground(bronze);
            play.setFocusable(false); // D-pad stays in actual thumbnail strip.
            actions.addView(play);

            live = new TextView(c);
            live.setText("●  AO VIVO");
            live.setTextSize(11);
            live.setTextColor(0xFFF6F2EB);
            live.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
            live.setPadding(dp(c,11),0,0,0);
            actions.addView(live);
            live.setVisibility(View.GONE);
        }

        void bind(Video video) {
            if (video == null) return;
            currentVideo = video;
            title.setText(video.getTitle());
            meta.setText(video.getSecondTitle());
            live.setVisibility(video.isLive ? View.VISIBLE : View.GONE);
            Context c = getContext();
            if (c instanceof Activity && ((Activity)c).isDestroyed()) return;
            Glide.with(c)
                .load(ClickbaitRemover.updateThumbnail(
                    video, MainUIData.instance(c).getThumbQuality()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.card_placeholder)
                .into(art);
        }

        void clear() {
            currentVideo=null;
            Glide.with(getContext().getApplicationContext()).clear(art);
            title.setText("");
            meta.setText("");
        }
    }

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        RowPresenter.ViewHolder holder=super.createRowViewHolder(parent);
        ListRowView rowView=(ListRowView)holder.view;
        FeaturedScene feature=new FeaturedScene(rowView.getContext());
        rowView.addView(feature,0);
        rowView.getGridView().setHorizontalSpacing(dp(rowView.getContext(),12));
        lastScene=new WeakReference<>(feature);
        return holder;
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder,Object item) {
        super.onBindRowViewHolder(holder,item);
        ListRow row=(ListRow)item;
        FeaturedScene feature=findScene(holder);
        currentRow=new WeakReference<>(row);
        if (feature!=null && row.getAdapter()!=null && row.getAdapter().size()>0) {
            Object video=row.getAdapter().get(0);
            if(video instanceof Video) feature.bind((Video)video);
        }
    }

    public void displaySelection(Video video) {
        FeaturedScene feature=lastScene.get();
        if(feature!=null && video!=null) feature.bind(video);
    }

    private FeaturedScene findScene(RowPresenter.ViewHolder holder) {
        ListRowView root=(ListRowView)holder.view;
        for(int i=0;i<root.getChildCount();i++){
            View child=root.getChildAt(i);
            if(child instanceof FeaturedScene) return (FeaturedScene)child;
        }
        return null;
    }

    @Override
    protected void onUnbindRowViewHolder(RowPresenter.ViewHolder holder) {
        FeaturedScene feature=findScene(holder);
        if(feature!=null) feature.clear();
        lastScene=new WeakReference<>(null);
        currentRow=new WeakReference<>(null);
        super.onUnbindRowViewHolder(holder);
    }
}
'''

def main():
    ap=argparse.ArgumentParser(); ap.add_argument("upstream",type=Path)
    root=ap.parse_args().upstream.resolve()
    fragment=root/ROWS
    text=fragment.read_text(encoding="utf-8")
    if 'mSpotlightPresenter' not in text:
        raise RuntimeError("HX first row not present. Apply previous patches first.")
    (root/PRES/"HxFeaturedRow.java").write_text(FEATURE_ROW,encoding="utf-8")
    (root/PRES/"HxEditorialMiniCardPresenter.java").write_text(MINI,encoding="utf-8")
    (root/PRES/"HxEditorialRowPresenter.java").write_text(PRESENTER,encoding="utf-8")

    replace_one(fragment,
        "import com.liskovsoft.smartyoutubetv2.tv.presenter.HxSpotlightCardPresenter;",
        "import com.liskovsoft.smartyoutubetv2.tv.presenter.HxSpotlightCardPresenter;\n"
        "import com.liskovsoft.smartyoutubetv2.tv.presenter.HxFeaturedRow;\n"
        "import com.liskovsoft.smartyoutubetv2.tv.presenter.HxEditorialRowPresenter;\n"
        "import com.liskovsoft.smartyoutubetv2.tv.presenter.HxEditorialMiniCardPresenter;")
    replace_one(fragment,
        "    private HxSpotlightCardPresenter mSpotlightPresenter;",
        "    private HxSpotlightCardPresenter mSpotlightPresenter;\n"
        "    private HxEditorialRowPresenter mEditorialPresenter;\n"
        "    private HxEditorialMiniCardPresenter mEditorialMini;")
    replace_one(fragment,
        "        mSpotlightPresenter = new HxSpotlightCardPresenter();",
        "        mSpotlightPresenter = new HxSpotlightCardPresenter();\n"
        "        mEditorialPresenter = new HxEditorialRowPresenter();\n"
        "        mEditorialMini = new HxEditorialMiniCardPresenter();")
    replace_one(fragment,
        "            presenterSelector.addClassPresenter(ListRow.class, mRowPresenter);",
        "            presenterSelector.addClassPresenter(ListRow.class, mRowPresenter);\n"
        "            presenterSelector.addClassPresenter(HxFeaturedRow.class, mEditorialPresenter);")
    replace_one(fragment,
        "        mSpotlightPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());",
        "        mSpotlightPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());\n"
        "        mEditorialMini.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());")
    replace_one(fragment,
        '                    isSpotlight ? mSpotlightPresenter : mCardPresenter);',
        '                    isSpotlight ? mEditorialMini : mCardPresenter);')
    replace_one(fragment,
        "            ListRow row = new ListRow(rowHeader, videoGroupAdapter);",
        "            ListRow row = isSpotlight ? new HxFeaturedRow(rowHeader, videoGroupAdapter)\n"
        "                    : new ListRow(rowHeader, videoGroupAdapter);")
    replace_one(fragment,
        "            if (item instanceof Video) {\n                mBackgroundManager.setBackgroundFrom((Video) item);",
        "            if (item instanceof Video) {\n"
        "                if(row instanceof HxFeaturedRow) {\n"
        "                    mEditorialPresenter.displaySelection((Video)item);\n"
        "                }\n"
        "                mBackgroundManager.setBackgroundFrom((Video) item);")
    dims=root/RES/"values/dimens.xml"
    s=dims.read_text(encoding="utf-8")
    if s.count("</resources>")!=1:raise RuntimeError("missing dimens root")
    dims.write_text(s.replace("</resources>",
        '    <dimen name="hx_mini_card_width">173dp</dimen>\n'
        '    <dimen name="hx_mini_card_height">98dp</dimen>\n</resources>',1),encoding="utf-8")
    ET.parse(dims)
    assert 'new HxFeaturedRow' in fragment.read_text(encoding="utf-8")
    print("HX TV 0.8: native editorial hero + smaller thumbnail carousel, same player.")
    print("HX TV 0.8: no screen-wide overlays, no WebView, no playback rewrite.")

if __name__=="__main__":main()
