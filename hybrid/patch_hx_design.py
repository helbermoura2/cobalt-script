#!/usr/bin/env python3
"""HX TV native theme + guarded updater after patch_smarttube.py.

Patches the pinned upstream SmartTube Android source, not the running YouTube
website. Changes no auto-scrolling / marquee text behavior and no video codec.
"""
from pathlib import Path
import argparse
import hashlib
import shutil
import xml.etree.ElementTree as ET

HX_CERT_SHA256 = "1e929c67bf65dd36a8875595f882089f036b4d8c68700a6f4fd70fb23efd86d7"
ALLOWED_RELEASE_PREFIX = ("https://github.com/helbermoura2/cobalt-script/releases/"
                          "download/hx-smarttube-signed-v")


def replace_once(path: Path, old: str, new: str):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Unexpected pinned-source marker {path}: {old[:90]!r} count={count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print("HX PATCH", path)


def append_resources(path: Path, insert: str):
    text = path.read_text(encoding="utf-8")
    if text.count("</resources>") != 1:
        raise RuntimeError(f"Unexpected Android resources: {path}")
    path.write_text(text.replace("</resources>", insert + "\n</resources>", 1), encoding="utf-8")
    ET.parse(path)
    print("HX XML", path)


def patch_theme(root: Path):
    # Existing apps store only the index (usually index 1). Replacing the
    # default gray entry IN PLACE means it becomes HX Bronze without resetting
    # user preferences. All other themes remain selectable in Settings.
    main_ui = root / ("common/src/main/java/com/liskovsoft/"
                      "smartyoutubetv2/common/prefs/MainUIData.java")
    replace_once(main_ui, '''                R.string.color_scheme_dark_grey,
                "App.Theme.DarkGrey.Player",
                "App.Theme.DarkGrey.Browse",
                "App.Theme.DarkGrey.Preferences",
                mContext));''', '''                R.string.hx_theme_bronze,
                "App.Theme.HX.Player",
                "App.Theme.HX.Browse",
                "App.Theme.HX.Preferences",
                mContext));''')

    common_strings = root / "common/src/main/res/values/strings.xml"
    replace_once(common_strings,
                 '<string name="check_for_updates">Check for updates</string>',
                 '<string name="hx_theme_bronze">HX TV — Bronze Imperial (OLED)</string>\n'
                 '    <string name="hx_update_safe_wait">Não há uma atualização assinada compatível disponível.</string>\n'
                 '    <string name="check_for_updates">Check for updates</string>')

    colors = root / "smarttubetv/src/main/res/values/colors.xml"
    append_resources(colors, '''
    <!-- HX theme: shared native TV resources, black OLED and warm bronze. -->
    <color name="hx_black">#FF000000</color>
    <color name="hx_panel">#FF12100F</color>
    <color name="hx_card">#FF1F1C1A</color>
    <color name="hx_bronze">#FFC99B64</color>
    <color name="hx_bronze_dark">#FF5B4129</color>
    <color name="hx_bronze_focus">#FFE0B985</color>
    <color name="hx_text">#FFF8F2E8</color>''')

    styles = root / "smarttubetv/src/main/res/values/styles.xml"
    append_resources(styles, '''
    <!-- HX native styles: selectable via Settings; replaces default gray. -->
    <style name="App.Theme.HX.Player" parent="App.Theme.Leanback.Player">
        <item name="android:windowBackground">@color/hx_black</item>
        <item name="shelfBackground">@color/hx_black</item>
        <item name="cardDefaultBackground">@color/hx_card</item>
        <item name="cardSelectedBackground">@color/hx_bronze_focus</item>
        <item name="playbackProgressPrimaryColor">@color/hx_bronze</item>
        <item name="playbackProgressSecondaryColor">@color/hx_bronze_dark</item>
        <item name="playbackControlsIconHighlightColor">@color/hx_bronze</item>
        <item name="android:colorAccent" tools:targetApi="21">@color/hx_bronze</item>
    </style>
    <style name="App.Theme.HX.Browse" parent="App.Theme.Leanback.Browse">
        <item name="android:windowBackground">@color/hx_black</item>
        <item name="brandColor">@color/hx_panel</item>
        <item name="brandAccentColor">@color/hx_bronze</item>
        <item name="shelfBackground">@color/hx_black</item>
        <item name="cardDefaultBackground">@color/hx_card</item>
        <item name="cardSelectedBackground">@color/hx_bronze_focus</item>
        <item name="appLogo">@mipmap/app_logo</item>
        <item name="android:colorAccent" tools:targetApi="21">@color/hx_bronze</item>
    </style>
    <style name="App.Theme.HX.Preferences" parent="App.Theme.Leanback.Preferences">
        <item name="android:colorPrimary" tools:targetApi="21">@color/hx_panel</item>
        <item name="android:colorAccent" tools:targetApi="21">@color/hx_bronze</item>
    </style>''')

    # Dark-gray and red fallback styles still refer to the upstream 'S beta'
    # logo. Replace resources with the already-generated provisional HX icon.
    folder = root / "smarttubetv/src/main/res/mipmap-nodpi"
    own_logo = folder / "app_logo.png"
    if not own_logo.exists():
        raise RuntimeError("HX logo not generated; run patch_smarttube.py first")
    for name in ("app_logo_semi_red.png", "app_logo_semi_grey.png",
                 "app_icon_alt.png"):
        shutil.copyfile(own_logo, folder / name)
    # Do not remove/upscale marquee animation: upstream LongClick/Focus
    # behavior already exposes the whole label on selection.

    assert "App.Theme.HX.Browse" in styles.read_text(encoding="utf-8")
    assert "R.string.hx_theme_bronze" in main_ui.read_text(encoding="utf-8")


def patch_updater(root: Path):
    # The upstream generic updater can receive an arbitrary APK URL.
    # Never invite the TV to install upstream/debug builds. Only permit our
    # signed-release channel and only from the established HX TV certificate.
    # Android's package installer remains responsible for final verification.
    presenter = root / ("common/src/main/java/com/liskovsoft/"
                        "smartyoutubetv2/common/app/presenters/dialogs/"
                        "AppUpdatePresenter.java")
    replace_once(presenter,
                 "import com.liskovsoft.appupdatechecker2.AppUpdateChecker;",
                 "import android.content.pm.PackageManager;\n"
                 "import android.content.pm.PackageInfo;\n"
                 "import android.content.pm.Signature;\n"
                 "import java.security.MessageDigest;\n"
                 "import com.liskovsoft.appupdatechecker2.AppUpdateChecker;")
    replace_once(presenter, '''    public void onUpdateFound(String versionName, List<String> changelog, String apkPath) {
        if (mIsForceCheck) {''',
                 '''    public void onUpdateFound(String versionName, List<String> changelog, String apkPath) {
        // HX: never offer an upstream SmartTube/debug APK or an APK to an
        // install not signed with our persistent key.
        if (!isCompatibleHxRelease(apkPath)) {
            if (mIsForceCheck) {
                LoadingManager.showLoading(getContext(), false);
                MessageHelpers.showMessage(getContext(), R.string.hx_update_safe_wait);
            }
            onFinish();
            return;
        }
        if (mIsForceCheck) {''')
    marker = '    private void showUpdateDialog(String versionName, List<String> changelog, String apkPath) {'
    method = '''
    private boolean isCompatibleHxRelease(String apkPath) {
        if (apkPath == null ||
                !apkPath.startsWith("''' + ALLOWED_RELEASE_PREFIX + '''") ||
                !apkPath.endsWith(".apk") || getContext() == null) {
            return false;
        }

        try {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(
                    getContext().getPackageName(), PackageManager.GET_SIGNATURES);
            if (packageInfo.signatures == null || packageInfo.signatures.length != 1) {
                return false;
            }
            Signature signature = packageInfo.signatures[0];
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray());
            StringBuilder hex = new StringBuilder();
            for (byte part : digest) {
                hex.append(String.format(java.util.Locale.ROOT, "%02x", part & 0xff));
            }
            return "''' + HX_CERT_SHA256 + '''".equals(hex.toString());
        } catch (Exception exception) {
            return false;
        }
    }

'''
    replace_once(presenter, marker, method + marker)
    assert "isCompatibleHxRelease(apkPath)" in presenter.read_text(encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("upstream", type=Path)
    args = parser.parse_args()
    root = args.upstream.resolve()
    if not (root / "smarttubetv/build.gradle").exists():
        raise RuntimeError("Expected SmartTube source checkout")
    patch_theme(root)
    patch_updater(root)
    print("HX TV: native Bronze OLED theme + provisional branding + signed-update guard applied")


if __name__ == "__main__":
    main()
