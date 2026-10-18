# Verification — October 18, 2026

## Passed

- **GitHub repository**: [97ryan/LuckyLotto](https://github.com/97ryan/LuckyLotto) created as a public repository after GitHub authorization was updated. Source and the automatic Android verification/APK build workflow are included.

- Android debug APK compilation (`assembleDebug`).
- Release compilation with R8 shrinking (`assembleRelease`); release APK is unsigned pending an owner-controlled key.
- **25 local tests, zero failures**: exact ENCORE overlap accounting, lottery combinatorics, KENO wagers, complete MEGADICE roll enumeration, budget optimization against brute force, ENCORE host cost and maximum count, cents/zero/overflow cases, elapsed draws, Toronto DST boundaries, stale jackpots and real recorded OLG feed parsing.
- Android lint: **zero errors**, with advisory dependency-version and Kotlin-convenience warnings. The latest stable Material 3 component version is used; the Compose runtime is pinned to a version compatible with the installed API 36 SDK.
- Debug APK signature verified with Android `apksigner` (v2).
- Package metadata verified: `org.luckylotto`, label LuckyLotto, version 0.1.0, minimum API 26, target API 36.
- Public 6/49 and LOTTO MAX endpoints verified with the same request headers used by the Android app; real draw dates, jackpots and MAXPLUS / MAXMILLIONS parsed. Other requested feeds returned HTTP 401 and remain explicit failures.

## Not completed

- **Device / visual smoke test**: the host Android emulator segfaulted before the app could launch. Reproduced with accelerated and CPU-only modes, different graphics modes and an isolated official stable emulator. The GitHub Actions workflow now runs a separate clean Android emulator smoke test and captures screenshots; its run result must be checked before publishing. Local device-test success is not claimed. Test on a physical Android device before a production release.
- **Complete all-game coverage**: the coverage table documents unmodeled games/strategies and prize tiers. Full all-game optimality is not claimed. No unauthorized OLG account endpoints or embedded website credentials were used.

The downloadable APK is a development build for testing. It has not been submitted to Google Play, F-Droid or a public release service.
