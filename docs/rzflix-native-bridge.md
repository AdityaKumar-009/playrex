# RzFlix native connector — verification status

**Status: adapter implemented; live RzFlix content API unverified.** This is not a working RzFlix catalog or stream service.

The uploaded RzFlix APK is a compiled Flutter client. Strings in its Dart AOT library include:

- `https://mainapi.yomoviesapk.com/r`
- `https://api.ringzstudio.com/apis/r.txt`
- An `_fetchOnDemandStream` client-side method and references to HTTP, WebViews, and third-party crawling.

Those two domains did not resolve in the 2026-10-09 external checks. Their presence in the APK does not reveal how to call catalog/search/metadata/stream endpoints. These may be **configuration discovery URLs**, rather than the API itself. We cannot responsibly infer other paths or reuse hidden credentials.

## Native implementation

`composeApp/src/commonMain/kotlin/com/nuvio/app/features/rzflix/RzFlixNativeBackend.kt` adds:

1. An injected HTTP client boundary, calling Nuvio's shared HTTP transport by default, for **confirmed HTTPS URLs only**.
2. Configuration-response discovery for an HTTPS URL, common JSON base URL aliases, or a Stremio-compatible manifest; unknown formats are explicitly unsupported.
3. Catalog + search JSON normalization to Nuvio `MetaPreview` objects.
4. Details / episode metadata normalization to Nuvio `MetaDetails` / `MetaVideo`.
5. Video stream and subtitle normalization to Nuvio `StreamItem` / `StreamSubtitle`. Only HTTPS URLs are accepted; JavaScript URLs, ad-click external URLs, and iframe/WebView popups are not opened.
6. Pure offline contract tests in `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/rzflix/RzFlixNativeBackendTest.kt`.

This is a **provider adapter, not an RzFlix server clone**. It is intentionally isolated: without a verified API contract, it does not install a broken provider into the Nuvio catalog, pretend that any title is playable, or contact the APK's candidate hosts automatically.

## Required evidence for exact RzFlix support

To wire this adapter into Nuvio's `CatalogRepository`, `MetaDetailsRepository`, `StreamsRepository` and `PlayerStreamsRepository`, we need legitimate, working response examples for:

- Configuration discovery request and body.
- Movie/series home catalog request and JSON body.
- Search request, encoding, and JSON body.
- Detail / season / episode request and response shape.
- Stream-list request and payload; direct playable URLs vs embed pages, permitted headers and expiry behavior.

An authorized API specification or **redacted** request/response captures from an account/device you control are suitable. Never include authentication cookies, bearer tokens, account IDs, DRM keys, or personal viewing history in captured fixtures. Provider access must be authorized, and only media the user is permitted to stream should be played.

Nuvio's upstream Compose UI is deliberately unchanged. The RzFlix-specific pipeline will only be enabled after verification; existing Nuvio add-ons remain functional.
