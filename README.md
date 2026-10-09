# Playrex — native Android media client

Playrex is an Android fork of [Nuvio Mobile 0.5.8-beta](https://github.com/NuvioMedia/NuvioMobile/releases/tag/0.5.8-beta) retaining Nuvio's Kotlin Multiplatform/Compose UI and native playback.

**Project status:** Nuvio UI + original user-configured add-on capabilities; **not yet connected to RzFlix's backend**. Static inspection of the uploaded RzFlix APK found candidate URLs but no verified public API contract, access permissions or JSON response schema. Neither RzFlix's Flutter binary nor its advertising SDKs are incorporated.

The first run of [Playrex Android workflow](.github/workflows/playrex-android.yml) imports the upstream GPLv3 Nuvio 0.5.8-beta source into this repository, applies the minimal Playrex application ID and launcher-name changes, audits for known ad SDKs, and attempts a native Android debug build.

After import, build with `./gradlew :androidApp:assembleFullDebug` (Android SDK + Java 17 required). Find any successful APK under Actions run artifacts.

RzFlix static analysis and integration boundaries: [docs/rzflix-analysis.md](docs/rzflix-analysis.md).

**License:** Nuvio source is GNU GPL v3.0. This fork preserves the Nuvio source and notices, and must continue to make corresponding GPLv3 source available. RzFlix proprietary binaries are not distributed. Do not assume an undocumented streaming API is approved for third-party use.
