#!/usr/bin/env python3
"""Verify that an Android APK doesn't bundle RzFlix's known ad SDKs.

This is a **binary dependency check**, not a claim that external media is ad-free.
Run: python3 tools/playrex/audit_apk.py androidApp/build/outputs/apk/full/debug/*.apk
"""
import re
import sys
from pathlib import Path
from zipfile import ZipFile, BadZipFile

SDK_SIGNATURES = {
    "TradPlus": b"com/tradplus",
    "Unity Ads": b"com/unity3d/ads",
    "Vungle": b"com/vungle/ads",
    "Mintegral / MBridge": b"com/mbridge",
    "Google Mobile Ads": b"com/google/android/gms/ads",
}

def audit(apk: Path) -> bool:
    try:
        with ZipFile(apk) as archive:
            names = archive.namelist()
            dex_names = [name for name in names if re.fullmatch(r"classes\d*\.dex", name)]
            if not dex_names:
                raise ValueError("APK has no classes.dex: cannot audit")
            native_flutter = [
                name for name in names if name.startswith("lib/") and name.endswith("/libflutter.so")
            ]
            found = set()
            for dex_name in dex_names:
                dex = archive.read(dex_name)
                for label, signature in SDK_SIGNATURES.items():
                    if signature in dex:
                        found.add(label)
            if native_flutter:
                found.add("Embedded RzFlix/Flutter runtime")
            if found:
                print(f"FAIL: {apk.name} bundles: {', '.join(sorted(found))}")
                return False
            print(f"PASS: {apk.name}: {len(dex_names)} DEX files; no known advertising SDK signatures or Flutter runtime")
            return True
    except (BadZipFile, ValueError, OSError) as exc:
        print(f"FAIL: cannot audit {apk}: {exc}")
        return False

if __name__ == "__main__":
    files = [Path(arg) for arg in sys.argv[1:]]
    if not files:
        sys.exit("Usage: audit_apk.py path/to/debug.apk [...]")
    sys.exit(0 if all(audit(path) for path in files) else 1)
