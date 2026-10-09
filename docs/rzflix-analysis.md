# RzFlix APK static assessment — October 9, 2026

**Source:** User-uploaded RzFlix APK, inspected offline. Findings are from APK ZIP entries and extracted native / DEX strings. This is NOT a live reverse-engineered API protocol, verified response mapping or complete application reconstruction.

## Evidence from the APK

- It packages multiple copies of `libflutter.so` and `libapp.so` — compiled Flutter/Dart code, not the original editable application source or server implementation.
- Android DEX classes contain `com.tradplus`, `com.unity3d.ads`, `com.vungle.ads` and `com.mbridge.msdk`. Those SDKs are plausible contributors to intrusive advertising.
- AOT strings include `_maybeShowInterstitial`, `showInterstitialAd`, `loadInterstitialAd`, `loadNativeAds`, `nativeAdReady` and `loadRewardedAd`. We cannot identify every display trigger without runtime instrumentation.
- Candidate URL strings: `https://mainapi.yomoviesapk.com/r` and `https://api.ringzstudio.com/apis/r.txt`. Neither was accessible for confirming response schema; a string in a binary does **not** prove the URL is a permitted or stable public API.
- The packaged app describes itself as a crawler / aggregator of third-party links. It does not establish which individual streams may be redistributed or played by another client.

## Implementation approach

- Preserve the supplied Nuvio Mobile 0.5.8-beta Kotlin/Compose Android frontend, search, details, playlists, player, settings and add-on integration.
- Use a distinct application ID and launcher name for Playrex, retaining GPLv3 and upstream copyright attribution.
- Exclude **all** RzFlix Flutter binary libraries, ad SDK dependencies and proprietary compiled code. Validate dependencies through `tools/playrex/audit_source.py`.
- Retain Nuvio's user-configured Stremio-compatible add-ons and existing supported media-server sources. They can supply metadata and playback where users have appropriate rights.
- Only add an RzFlix-specific provider after the API owner authorizes access and actual response fixtures / authentication / transport behavior are verified; map them to Nuvio's `AddonRepository`, `StreamsRepository` and `PlayerStreamsRepository` rather than replacing the Compose frontend.

## Outstanding for RzFlix interoperability

The APK does **not** provide a verified server implementation, API documentation or tested `catalog`, `meta`, `search`, and `stream` response contracts. Never ship speculative endpoint paths, guessed credentials, unauthorized content scraping or intrusive external redirects. Current Playrex is a native, ad-SDK-free UI source fork, **not a verified RzFlix backend port**.

## Nuvio source license

[NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) is GPL-3.0, and the original `LICENSE` and notices must be retained in the fork.
