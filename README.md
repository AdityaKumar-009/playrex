# Playrex — Native Android media client

**Status (9 Oct 2026):** A native Android fork of the full [Nuvio Mobile 0.5.8-beta](https://github.com/NuvioMedia/NuvioMobile/releases/tag/0.5.8-beta) Kotlin/Compose frontend. A Kotlin RzFlix-compatible JSON provider adapter and offline tests have been added, but the **live RzFlix backend is not yet connected** because the original API contract, working server endpoints and permitted access have not been verified.

## What's included

- Nuvio 0.5.8-beta UI screens, navigation, catalog, search, title details, playback, settings, and its user-configured Stremio-compatible add-on support.
- `com.playrex.app` Android application ID, `com.playrex.app.debug` for debug, and the **Playrex** Android launcher label.
- Source audit guarding against importing RzFlix's **TradPlus, Unity Ads, Vungle and MBridge** advertising SDKs. No RzFlix APK or Flutter binaries are copied into the Android project.
- [RzFlix APK analysis and unverified-backend boundaries](docs/rzflix-analysis.md).
- [Native RzFlix JSON adapter, typed media models, HTTPS validation and outstanding verification](docs/rzflix-native-bridge.md).

## Build / APK

Go to [GitHub Actions](https://github.com/AdityaKumar-009/playrex/actions/workflows/playrex-android.yml) and inspect the latest run. A **successful** build publishes a `playrex-android-full-debug` artifact containing a debug APK. This is not a signed Play Store release.

To compile locally with Android Studio/Android SDK and Java 17:

```bash
./gradlew :androidApp:assembleFullDebug
```

The project originates from the **exact Nuvio 0.5.8-beta tag**, with its original Compose layouts retained. Do not assume the app can serve RzFlix catalogs or streams: candidate RzFlix server URLs embedded in the APK were not accessible for confirming their API contracts. A documented, authorized API and redacted response fixtures are required before implementing that provider in the existing Nuvio catalog/stream repository interfaces.

## Credits, legal and privacy

The source is a modified derivative of [NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile), licensed under **GNU GPL v3.0**; the original `LICENSE` and relevant notices remain. This repo distributes source so downstream users can inspect and modify it. Nuvio's own optional network integrations still exist; removing RzFlix ad SDKs does not guarantee that every externally supplied media page is free of ads. Playback should use sources you are authorized to access.
