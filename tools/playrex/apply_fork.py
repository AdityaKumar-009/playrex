#!/usr/bin/env python3
"""Apply minimal, idempotent Playrex Android branding to Nuvio 0.5.8-beta.

Keep Nuvio's Compose layouts, navigation and playback intact.
Do not include RzFlix binaries, ad libraries or private API credentials.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_exact(relative_path: str, original: str, new: str) -> None:
    path = ROOT / relative_path
    if not path.is_file():
        raise SystemExit(f"Missing required Nuvio source: {relative_path}")
    content = path.read_text(encoding="utf-8")
    if new in content and original not in content:
        return
    if content.count(original) != 1:
        raise SystemExit(
            f"Expected one original marker in {relative_path}; found {content.count(original)}"
        )
    path.write_text(content.replace(original, new), encoding="utf-8")


def main() -> None:
    version = ROOT / "iosApp/Configuration/Version.xcconfig"
    if not version.exists() or "MARKETING_VERSION=0.5.8" not in version.read_text():
        raise SystemExit("Expected Nuvio Mobile 0.5.8 source")
    replace_exact("androidApp/build.gradle.kts",
                  'applicationId = "com.nuvio.app"',
                  'applicationId = "com.playrex.app"')
    replace_exact("androidApp/build.gradle.kts",
                  'variant.applicationId.set("com.nuviodebug.com")',
                  'variant.applicationId.set("com.playrex.app.debug")')
    replace_exact("composeApp/src/androidMain/res/values/strings.xml",
                  '<string name="app_name">Nuvio</string>',
                  '<string name="app_name">Playrex</string>')
    # Keep upstream Gradle rootProject.name = "Nuvio". Compose generated-resource
    # package names are derived from it and referenced throughout the UI.
    print("Playrex application ID/label updated. Original Nuvio UI retained.")


if __name__ == "__main__":
    main()
