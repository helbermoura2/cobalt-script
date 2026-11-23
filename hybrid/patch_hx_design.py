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
                 '<string name="hx_theme_bronze">HX TV — Signature Graphite (OLED)</string>\n'
                 '    <string name="hx_update_safe_wait">Não há uma atualização assinada compatível disponível.</string>\n'
                 '    <string name="check_for_updates">Check for updates</string>')

    colors = root / "smarttubetv/src/main/res/values/colors.xml"
    append_resources(colors, '''
    <!-- HX theme: shared native TV resources, black OLED, graphite and cool silver-blue. -->
    <color name="hx_black">#FF000000</color>
    <color name="hx_panel">#FF101722</color>
    <color name="hx_card">#FF1D2836</color>
    <color name="hx_bronze">#FFB5D7FF</color>
    <color name="hx_bronze_dark">#FF314860</color>
    <color name="hx_bronze_focus">#FFF0F7FF</color>
    <color name="hx_text">#FFF7F9FD</color>''')

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
    # The SmartTube update callback receives a local file path, not a remote URL.
    # Verify installed and downloaded APKs with the permanent HX certificate.
    # The Android package installer independently verifies the final APK.
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
        // HX: validate the downloaded APK bytes before presenting Install.
        if (!isCompatibleHxRelease(apkPath, versionName)) {
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
    // The upstream AppUpdateChecker downloads the APK FIRST and invokes
    // onUpdateFound(..., apkPath) with a LOCAL FILE PATH, not the release URL.
    // Hence validate the actual downloaded APK signature and package, never
    // require a GitHub URL here.
    private boolean isCompatibleHxRelease(String apkPath, String versionName) {
        if (apkPath == null || getContext() == null ||
                !new java.io.File(apkPath).isFile() ||
                new java.io.File(apkPath).length() < 1_000_000L) {
            return false;
        }
        try {
            PackageManager pm = getContext().getPackageManager();
            PackageInfo installed = pm.getPackageInfo(
                    getContext().getPackageName(), PackageManager.GET_SIGNATURES);
            PackageInfo downloaded = pm.getPackageArchiveInfo(
                    apkPath, PackageManager.GET_SIGNATURES);

            if (downloaded == null || installed == null ||
                    !getContext().getPackageName().equals(downloaded.packageName) ||
                    !installed.packageName.equals(downloaded.packageName) ||
                    downloaded.applicationInfo == null ||
                    (downloaded.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 ||
                    !android.text.TextUtils.equals(versionName, downloaded.versionName) ||
                    downloaded.versionCode <= installed.versionCode ||
                    !hasCompatibleNativeLibraries(apkPath)) {
                return false;
            }

            // Fail closed unless both installed AND downloaded packages use
            // exactly our persistent HX TV signing certificate. Android's
            // PackageInstaller will independently verify the final update.
            return hasHxSigningCertificate(installed) &&
                    hasHxSigningCertificate(downloaded);
        } catch (Exception exception) {
            android.util.Log.w("HX-TV-Updater", "Rejecting incompatible update: " +
                    exception.getClass().getSimpleName());
            return false;
        }
    }

    private boolean hasCompatibleNativeLibraries(String apkPath) throws java.io.IOException {
        try (java.util.zip.ZipFile archive = new java.util.zip.ZipFile(apkPath)) {
            for (java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = archive.entries();
                    entries.hasMoreElements();) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".so")) continue;
                for (String abi : android.os.Build.SUPPORTED_ABIS) {
                    if (name.startsWith("lib/" + abi + "/")) return true;
                }
            }
        }
        return false;
    }

    private boolean hasHxSigningCertificate(PackageInfo packageInfo) throws Exception {
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
    }

'''
    replace_once(presenter, marker, method + marker)
    assert "isCompatibleHxRelease(apkPath, versionName)" in presenter.read_text(encoding="utf-8")
    assert "getPackageArchiveInfo(" in presenter.read_text(encoding="utf-8")
    assert 'apkPath.startsWith(' not in presenter.read_text(encoding="utf-8")


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
