#!/usr/bin/env python3
"""HX Native 0.1.0 APK: patch only a pinned native URL and same-length manifest identity.
For personal testing on Android TV. Original source: reisxd/TizenTubeCobalt v2.0.3.
The upstream web application and userscript remain separate.
"""
from pathlib import Path
import argparse
import hashlib
import re
import shutil
import subprocess
import tempfile
import urllib.request
import zipfile

ORIGINAL_URL = b"https://cdn.jsdelivr.net/npm/@foxreis/tizentube/dist/userScript.js?v="
NEW_URL = b"https://cdn.jsdelivr.net/gh/helbermoura2/cobalt-script@hn1/t.js?v="
OLD_PACKAGE = "io.gh.reisxd.tizentube.cobalt"
NEW_PACKAGE = "io.gh.reisxd.tizentube.cobhn1"
APK_URL = "https://github.com/reisxd/TizenTubeCobalt/releases/download/v2.0.3/cobalt-arm.apk"
ABI = "armeabi-v7a"

def check(condition, message):
    if not condition:
        raise RuntimeError(message)

def run(*args):
    subprocess.run(list(map(str,args)), check=True)

def download(dest):
    req = urllib.request.Request(APK_URL, headers={"User-Agent": "HX-Native-010-builder"})
    with urllib.request.urlopen(req, timeout=90) as src, dest.open("wb") as out:
        while True:
            block = src.read(1024 * 1024)
            if not block: break
            out.write(block)
    check(dest.stat().st_size > 50_000_000, "Upstream APK is unexpectedly small")

def rewrite_manifest(raw):
    # AXML string pools may encode package identifiers as UTF-8 or UTF-16LE.
    # Equal byte lengths mean string offsets, compiled resource references and
    # the overall AXML structure remain unchanged.
    out = raw
    replaced = 0
    for encoding in ("utf-8", "utf-16le"):
        original = OLD_PACKAGE.encode(encoding)
        replacement = NEW_PACKAGE.encode(encoding)
        check(len(original) == len(replacement), "Package lengths differ")
        replaced += out.count(original)
        out = out.replace(original, replacement)
    check(replaced > 0, "Expected package identifier absent in binary manifest")
    return out, replaced

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--zipalign", default="zipalign")
    p.add_argument("--apksigner", default="apksigner")
    p.add_argument("--keytool", default="keytool")
    opts=p.parse_args()
    opts.output.parent.mkdir(parents=True, exist_ok=True)
    check(len(ORIGINAL_URL)==69, "Upstream native URL slot changed")
    check(len(NEW_URL)<=69, "HX controller URL exceeds native slot")
    check(len(OLD_PACKAGE)==len(NEW_PACKAGE), "Package identifiers must be same length")
    with tempfile.TemporaryDirectory(prefix="cobalt-hx-") as temp:
        temp=Path(temp)
        official=temp/"original.apk"
        draft=temp/"draft.apk"
        aligned=temp/"aligned.apk"
        key=temp/"signing.jks"
        download(official)
        original_url_matches=0
        changed_library=0
        replaced_manifest=0
        with zipfile.ZipFile(official) as inp, zipfile.ZipFile(draft,"w",allowZip64=True) as out:
            members=inp.infolist()
            check("AndroidManifest.xml" in inp.namelist(), "Manifest missing")
            check(len(members)==len(set(member.filename for member in members)), "Duplicate ZIP member")
            for item in members:
                name=item.filename
                if re.match(r"META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC)|SIG-[^/]+)$",name,re.I):
                    continue
                data=inp.read(item)
                if name == "AndroidManifest.xml":
                    data,replaced_manifest=rewrite_manifest(data)
                elif re.fullmatch(r"lib/[^/]+/[^/]+\.so",name):
                    matches=data.count(ORIGINAL_URL)
                    if matches:
                        check(name.startswith("lib/"+ABI+"/"),"Native URL exists in unexpected ABI")
                        check(matches==1,"Ambiguous native URL slot")
                        # Native C-string is followed by a NUL terminator.
                        idx=data.index(ORIGINAL_URL)
                        check(data[idx+len(ORIGINAL_URL):idx+len(ORIGINAL_URL)+1]==b"\x00","Original URL is not NUL-terminated")
                        padded=NEW_URL.ljust(len(ORIGINAL_URL),b"0")
                        data=data[:idx]+padded+data[idx+len(ORIGINAL_URL):]
                        original_url_matches+=matches
                        changed_library+=1
                out.writestr(item,data)
        check(original_url_matches==1 and changed_library==1,"Expected exactly one native URL")
        check(replaced_manifest>0,"Manifest identity not changed")
        with zipfile.ZipFile(draft) as z:
            check(z.testzip() is None,"Draft ZIP CRC invalid")
        run(opts.zipalign,"-p","-f","4",draft,aligned)
        # Test-only signature; the visual updater works remotely. A secure,
        # stable app signing key must be configured for future native OTA builds.
        password="cobalt-hx-preview-test-only"
        run(opts.keytool,"-genkeypair","-noprompt","-keystore",key,
            "-storepass",password,"-keypass",password,"-alias","hx-preview",
            "-keyalg","RSA","-keysize","3072","-validity","3650",
            "-dname","CN=HX Native 0.1.0 test,O=Personal TV test,C=BR")
        run(opts.apksigner,"sign","--ks",key,"--ks-key-alias","hx-preview",
            "--ks-pass","pass:"+password,"--key-pass","pass:"+password,
            "--v1-signing-enabled","true","--v2-signing-enabled","true",
            "--v3-signing-enabled","true","--out",opts.output,aligned)
        run(opts.apksigner,"verify","--verbose","--print-certs",opts.output)
        print("READY",opts.output,"size",opts.output.stat().st_size,
              "sha256",hashlib.sha256(opts.output.read_bytes()).hexdigest(),
              "manifest_replacements",replaced_manifest)
if __name__=="__main__":
    main()
