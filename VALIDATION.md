# PIXLO validation

## Version 2.0.1 — 12 October 2026

- Local JVM tests, lint and debug APK assembly passed. All 12 JVM tests passed with zero failures/errors.
- Widget taps toggle today's check-in. First day: 0/1. Second consecutive day: 1/2. Undo restores the saved previous streak rather than resetting it.
- Niwas tested the update on his phone and reported the toggle working. Overnight/date-rollover behaviour was not independently observed.
- Two added Android persistence checks were not compiled or executed in this local build; the optional GitHub emulator job can run them.
- Version name 2.0.1, version code 3. The historical section below refers to version 2.0.

## Version 2.0 — 9 October 2026

## Passed locally

Final command: `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` using portable Java 17, Gradle 8.9, Android SDK/build-tools 35. Android Studio was not used.

- **Application and Android test APKs compiled successfully.** The final run completed with `BUILD SUCCESSFUL`.
- **8 JVM unit tests passed**, with 0 failures and 0 errors. Coverage: no-repeat random rotation, empty/single libraries, interval bounds, idempotent same-day taps, consecutive-day increment, missed-day reset, independent habits, backward clock handling, ordinary/leap-year boundaries, footer mathematics and daily quote selection.
- **Android lint passed: 0 errors, 8 warnings.** Seven indicate newer dependency versions exist; one recommends asynchronous preference writes. Widget count/configuration saves deliberately use synchronous `commit()` for durability before the broadcast/configuration finishes. No lint baseline or blanket disabled checks was used.
- **APK signature verified**, and the signing certificate matches the previously delivered local PIXLO 1.0 APK. This permits an in-place update without clearing its photos/settings. It does not establish compatibility with APKs signed by separate cloud runs.
- **APK metadata:** `com.pixlo.app`, version 2.0, versionCode 2, min API 28, target API 35.
- **APK size:** 75,216,514 bytes. This includes the Android UI and on-device vision libraries across supported architectures.
- **Five live Commons genre API searches returned results**, 12 each in the development check. A supported sample quotation-image URL returned HTTP 200 and included a source page. This verifies the provider endpoints, not an Android-device download or the genre quality of every result.
- Android XML/resources and widget declarations compiled successfully.

Delivered `PIXLO-v2-debug.apk` SHA-256:

```
4464175f422e4b1e2be677a40a2b93c7ca628f7e983e8eec94e7cf5f7d895c71
```

## Permissions

The compiled APK requests Internet, network state and boot completion. WorkManager adds wake-lock and foreground-service permissions; AndroidX adds its internal signature-protected receiver permission. No broad photo/storage, camera or exact-alarm permission is requested. Photo import uses the system picker. Personal photos are not sent to the Commons image service; object processing is on-device after its initial ML Kit setup.

## Still requires device testing

- Installation/update and all actual Compose interactions.
- Photo effect appearance, transition smoothness, cropping and launcher wallpaper behavior.
- ML Kit initial download, detection quality and subject-aware layout on real photos.
- In-app Commons downloading, offline cache use and relevance for each genre. Existing internet-image lettering is not editable; personal-photo quotes support font controls.
- Pinning/resizing multiple widgets, configuration return behavior, broadcast delivery and overnight reset on your launcher.
- Local-date rollover, timezone changes, reboot/force-stop recovery and manufacturer battery restrictions.

The four Android bitmap instrumentation tests **compiled but were not executed** here. Enable the optional GitHub `device_tests` job to run them in an emulator. No physical phone or emulator was attached, and no GitHub repository was published or workflow dispatched on the user's account.

## Product boundaries

Internet images are source-backed Commons search results, not a guaranteed curated feed. A small cached collection can repeat; sources can fail or be rate limited. Personal-photo text comes from seven original lines per genre, cycling weekly. Automatic object avoidance is best effort, with manual placement and texture fallback. Widget background refresh can be delayed while asleep; the next tap always recalculates the current local date. See README for exact behaviors and phone checks.

The footer preserves the requested `70D left : 85%` **format**, with consistent values: percentage means year completed, so 70 days left in a 365-day year displays `70D left : 81%`.
