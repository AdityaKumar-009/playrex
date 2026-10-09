# RzFlix → Playrex: reproducible live protocol verification

**Why this is needed:** The RzFlix APK is a compiled Flutter/Dart client. Static examination found `getAllMovies`, `getEndpointsForSelection`, `_fetchOnDemandStream`, `FirebaseRemoteConfig`, and two embedded URL strings, but no accompanying server or tested request/response contract. The two strings are not proof of today's live API hosts. On-device RzFlix screenshots demonstrate its visible interface, not its HTTPS requests.

The supplied screenshots show a populated Movies featured banner, search suggestions, Downloads/More screens, and an error in Live. They do not establish that every title or Live stream plays successfully or whether the featured banner was cached.

## Minimal Android capture (one movie, no personal data)

1. Use [PCAPdroid](https://github.com/emanuele-f/PCAPdroid) to capture connections for the **RzFlix app only**. Start without TLS decryption.
2. Force-stop RzFlix, then start a short capture; launch RzFlix and visit **Movies**, search for a **new** title, open its details, and press Watch (only for content you are allowed to access).
3. Save the connection list or PCAP. Record the **requested hosts** and which hosts were contacted during Home, search, details, and playback.
4. If the app and service permissions allow, and the app trusts the locally installed certificate, PCAPdroid supports optional TLS decryption and HTTP request export as **HAR** (see its [TLS-decryption docs](https://emanuele-f.github.io/PCAPdroid/tls_decryption.html)). Do not weaken certificate pinning or circumvent any service's access controls. Capture only your own authorized traffic.
5. Before sharing, **remove** request/response headers such as `Authorization`, `Cookie`, `Set-Cookie`, `X-API-Key`, device IDs, signed stream URLs, tokens, account identifiers, analytics identifiers, searches containing personal information, and playback history.

At minimum, a safe plain-text summary can include one non-secret hostname plus request **method and path** (with private query parameter values removed), status code, `Content-Type`, and top-level JSON field names for each of catalog, search, details and streams. Redacted example values for one representative movie and one episode will be needed for accurate field mapping.

**Avoid publishing raw HAR/PCAP files** in this public repository. They can contain login/session credentials and private browsing data.

## Integration checkpoints

- [x] RzFlix APK inspected; intrusive third-party advertising SDKs identified.
- [x] Native Kotlin/Compose Nuvio UI fork compiled as Playrex without importing those SDKs.
- [x] Typed Kotlin catalog/search/details/streams normalization adapter created.
- [ ] Capture a *working* RzFlix request/response pair for each resource type.
- [ ] Determine its current API root, authentication requirements, fallbacks and legally authorized access.
- [ ] Register an authenticated/authorized RzFlix provider through the Nuvio source repository, add Home/Search/Details/Streams routes, with actual request fixtures.
- [ ] Validate search, playback, episode selection and the failure modes on-device; confirm absence of client-originated interstitial/pop-up ads.

**Do not** hardcode speculative API paths, ship RzFlix's ad SDKs, embed a web page as the main app, or describe a metadata-only Nuvio catalog as RzFlix streams. Unsupported Live channels should fail gracefully, not display fake content.
