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
LABEL = "HX TV"
VERSION_NAME = "0.9.16"
VERSION_CODE = 2494
RELEASE_TAG = "hx-tv-independent-v0.9.16-beta"
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
        im = Image.new("RGB", size, "#070D17")
        d = ImageDraw.Draw(im)
        w, h = size
        for i in range(h):
            v = i / max(h, 1)
            c = (int(11+v*19), int(12+v*10), int(15+v*8))
            d.line((0, i, w, i), fill=c)
        inset=int(min(w,h)*0.10)
        d.rounded_rectangle((inset,inset,w-inset,h-inset),radius=int(min(w,h)*.16),outline="#A8C5E5",width=max(3,w//100))
        font=ImageFont.truetype(font_path, int(min(w,h)*(.27 if not banner else .25)))
        label="HX"
        bbox=d.textbbox((0,0),label,font=font)
        tw=bbox[2]-bbox[0];th=bbox[3]-bbox[1]
        d.text(((w-tw)/2,(h-th)/2-bbox[1]), label, fill="#F4F8FF", font=font)
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
    # A interface HX experimental recebe atualizações assinadas num canal próprio.
    # Não altera o canal estável (native.json) usado por versões anteriores.
    url=f"https://raw.githubusercontent.com/{REPO}/smart-cobalt-hybrid/hybrid/updates/native-beta.json"
    updates=root/"common/src/stbeta/res/values/update_urls.xml"
    updates.write_text('<?xml version="1.0" encoding="utf-8"?>\n'
                       '<resources>\n    <string-array name="update_urls">\n'
                       f'        <item>{url}</item>\n'
                       '    </string-array>\n</resources>\n',encoding="utf-8")
    # The exact native Settings list is used by every screen; there is no
    # HTML overlay. The upstream app routes unrecognized package IDs to a
    # simplified "About" panel that hides AppUpdateChecker, so fix that route
    # and add our own updater entry prominently.
    setting_items=(root/"common/src/main/java/com/liskovsoft/smartyoutubetv2/common/misc/AppDataSourceManager.java")
    # A tela HX pertence ao módulo smarttubetv e não pode acessar diretamente
    # a classe AppUpdatePresenter (dependência appupdatechecker2 não transitiva).
    # Expor uma ponte pública no módulo common sem alterar seu instalador.
    replace_exact(setting_items,
                  "public class AppDataSourceManager {",
                  """public class AppDataSourceManager {
    public static void verificarAtualizacaoHx(Context context) {
        AppUpdatePresenter.instance(context).start(true);
    }""")
    replace_exact(setting_items,
                  "import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AboutSettingsPresenter;",
                  "import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AboutSettingsPresenter;\n"
                  "import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AppUpdatePresenter;")
    replace_exact(setting_items,
                  "        List<SettingsItem> settingItems = new ArrayList<>();",
                  "        List<SettingsItem> settingItems = new ArrayList<>();\n"
                  "        // HX: show the native app updater at the top of Settings.\n"
                  "        settingItems.add(new SettingsItem(\n"
                  "                context.getString(R.string.hx_update_app),\n"
                  "                () -> AppUpdatePresenter.instance(context).start(true),\n"
                  "                R.drawable.settings_about));")
    replace_exact(setting_items,
                  '''        if (Helpers.equalsAny(context.getPackageName(), Utils.KNOWN_PACKAGES)) {
            settingItems.add(new SettingsItem(
                    context.getString(R.string.settings_about), () -> AboutSettingsPresenter.instance(context).show(), R.drawable.settings_about));
        } else {
            settingItems.add(new SettingsItem(
                    context.getString(R.string.settings_about), () -> AboutSimpleSettingsPresenter.instance(context).show(), R.drawable.settings_about));
        }''',
                  '''        // HX has a dedicated update manifest; always expose the full About page.
        settingItems.add(new SettingsItem(
                context.getString(R.string.settings_about), () -> AboutSettingsPresenter.instance(context).show(), R.drawable.settings_about));''')

    common_strings=root/"common/src/main/res/values/strings.xml"
    common_text=common_strings.read_text(encoding="utf-8")
    assert "<resources xmlns:tools" in common_text
    assert 'name="hx_update_app"' not in common_text
    common_text=common_text.replace(
        '    <string name="check_for_updates">Check for updates</string>',
        '    <string name="hx_update_app">Atualizar aplicativo</string>\n'
        '    <string name="check_for_updates">Check for updates</string>',1)
    common_strings.write_text(common_text,encoding="utf-8")

    # Native Android TV resources for an actual OLED/bronze visual direction.
    # Keep card foreground bright for accessibility and do not touch the video decoder.
    colors=root/"smarttubetv/src/main/res/values/colors.xml"
    for old,new in (
        ('<color name="fastlane_background">#0096a6</color>', '<color name="fastlane_background">#B5D7FF</color>'),
        ('<color name="fastlane_background_dark">#004B53</color>', '<color name="fastlane_background_dark">#17283A</color>'),
        ('<color name="search_opaque">#0096a6</color>', '<color name="search_opaque">#8EBFEF</color>'),
        ('<color name="card_default_background">#0096a6</color>', '<color name="card_default_background">#17212F</color>'),
        ('<color name="card_default_background_dark">#004B53</color>', '<color name="card_default_background_dark">#101722</color>'),
        ('<color name="shelf_background">#282828</color>', '<color name="shelf_background">#111112</color>'),
        ('<color name="shelf_background_dark">#131313</color>', '<color name="shelf_background_dark">#080809</color>'),
        ('<color name="scheme_grey_background">#181818</color>', '<color name="scheme_grey_background">#101011</color>'),
        ('<color name="scheme_grey_background_dark">#181818</color>', '<color name="scheme_grey_background_dark">#0A0A0B</color>'),
        ('<color name="scheme_grey_card_background_dark">#121212</color>', '<color name="scheme_grey_card_background_dark">#171719</color>'),
        ('<color name="card_selected_background_yellow">#ffaa3f</color>', '<color name="card_selected_background_yellow">#DBEBFF</color>'),
        ('<color name="icon_background">#4A4F51</color>', '<color name="icon_background">#182637</color>'),
        ('<color name="orb_color">@color/grey</color>', '<color name="orb_color">@color/fastlane_background_dark</color>'),
        ('<color name="orb_bright_color">@color/grey</color>', '<color name="orb_bright_color">@color/fastlane_background</color>'),
    ):
        replace_exact(colors,old,new)
    shared_colors=root/"common/src/main/res/values/colors.xml"
    replace_exact(shared_colors,
                  '<color name="leanback">#0096A6</color>',
                  '<color name="leanback">#B5D7FF</color>')

    metrics=root/"smarttubetv/src/main/res/values/dimens.xml"
    replace_exact(metrics,'<dimen name="card_width">200dp</dimen>','<dimen name="card_width">218dp</dimen>')
    replace_exact(metrics,'<dimen name="card_height">110dp</dimen>','<dimen name="card_height">122dp</dimen>')
    replace_exact(metrics,'<dimen name="lb_basic_card_title_text_size">16sp</dimen>','<dimen name="lb_basic_card_title_text_size">18sp</dimen>')
    # This dimension also appears in a commented block upstream. Replace the
    # actual declaration, not the documented/commented example.
    dimen_source=metrics.read_text(encoding="utf-8")
    old='<dimen name="lb_basic_card_content_text_size">12sp</dimen>'
    assert dimen_source.count(old)==2
    i=dimen_source.rfind(old)
    metrics.write_text(dimen_source[:i]+
                       '<dimen name="lb_basic_card_content_text_size">13sp</dimen>'+
                       dimen_source[i+len(old):],encoding="utf-8")
    replace_exact(metrics,'<dimen name="grid_item_horizontal_spacing">15dp</dimen>','<dimen name="grid_item_horizontal_spacing">19dp</dimen>')

    # Sanity-check our native modifications; fail CI rather than publishing a
    # misleading APK if upstream code changes underneath the pinned revision.
    assert 'hx_update_app' in setting_items.read_text(encoding="utf-8")
    assert 'AppUpdatePresenter.instance(context).start(true)' in setting_items.read_text(encoding="utf-8")
    assert 'name="hx_update_app"' in common_strings.read_text(encoding="utf-8")
    assert 'name="fastlane_background">#B5D7FF' in colors.read_text(encoding="utf-8")

    # Disable upstream Google/Firebase tracking for our development preview.
    google=root/"smarttubetv/google-services.json"
    if google.exists():
        google.unlink()
        print("REMOVED upstream google-services.json")

    make_icons(root)
    # Output compatibility checks.
    b=build.read_text(encoding="utf-8")
    assert PACKAGE_ID in b and f'versionName "{VERSION_NAME}"' in b
    assert "enableVideoPreview(selected);" not in card.read_text(encoding="utf-8")
    assert url in updates.read_text(encoding="utf-8")
    print("HX Native TV source patch validated; updater manifest =",url)


if __name__=="__main__":
    main()
