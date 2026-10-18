# Android release

The included debug APK is for testing. A production release should use an owner-controlled signing key so future versions can update the same installation.

1. Review every official game rule, update `Catalog.kt`, the review date and source list. Complete or disclose remaining unsupported prize distributions.
2. Run the numeric tests, Android lint and emulator/device checks, including offline mode, cached values, input limits, dark mode, large text, disabled games, date rollover and Toronto daylight-saving changes.
3. Set the intended application ID before first production distribution (`org.luckylotto` is the current independent project ID); increment versionCode/versionName.
4. Generate or select a signing key under the owner's control. Store it outside this repository and back it up securely. A CI signing key, if later configured, belongs in encrypted secrets, never source.
5. Build `./gradlew assembleRelease`; sign with Android Studio or `apksigner` using the owner key. Verify with `apksigner verify --verbose` and publish the SHA-256 digest.
6. Attach corresponding source and GPL license to the same release. Test an upgrade from the prior signed version.

The current workflow builds a debug APK and never publishes a release or sends a signing key anywhere. No Google Play or F-Droid listing has been created.
