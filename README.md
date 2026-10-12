# PIXLO 2.0.1

Android 9+ app with three live wallpaper modes and multiple independent daily streak widgets. Free browser editing/building through GitHub Actions; no Android Studio, API key, backend, account inside the app, or paid AI service is required.

## Install this update

Version 2.0.1 adds reversible widget check-ins. Niwas tested the updated widget on his phone and confirmed it working. The local build passed 12 JVM tests, lint and APK assembly. See [VALIDATION.md](VALIDATION.md) for what was and was not verified.

For a fresh build from this repository, use **Actions → Build PIXLO APK** and download the APK artifact when the run succeeds. The following paragraph describes the previously delivered local v2 APK; cloud signing may differ.

Send `PIXLO-v2-debug.apk` to your phone and tap it. Allow **Install unknown apps** for the Files/browser app if Android asks. This local update uses the previous PIXLO debug signing key, so Android should offer an update preserving your imported photos and settings. If you previously installed a separately generated cloud APK, its debug key may differ; an incompatible-signature error requires uninstalling that copy first, which clears its private data.

## Four modes

### Photo wallpapers

Open **Photo wallpapers**, import up to 50 photos, choose Original, Pixel, Cartoon, Color paint, Doodle or Sketch. All library photos use the chosen filter. Pixel block size is adjustable. Filters are deterministic image-processing effects, not cloud generative AI. Select Fade, Slide, Zoom or None, enable random rotation, and choose 10/30/60/custom seconds (1–86400). Tap **Set live wallpaper**, confirm Android's preview, then return to the home screen. Hidden wallpapers pause their timer. Each transition lasts around 650 ms; intervals are approximate under load.

### Daily quotes

Select Work, Gym, Time, Study or Heartbreak.

- **Internet wallpapers:** tap **Download / refresh** for the selected genre. PIXLO searches Wikimedia Commons for motivational quotation images, downloads up to eight supported images into private storage, preserves their existing text, and displays one per local calendar day. Search relevance and availability depend on Commons; some genres may have few matching images or no usable results. The UI reports failures and keeps the previous cache. **View image source and license** shows the original file page, creator and license. Internet images are fitted without cutting off their quotation text. Their baked-in fonts cannot be edited. Background refresh is best effort; cached daily rotation works offline, and a short collection repeats after its last image.
- **Your photos:** the app creates a daily wallpaper using an original genre-based quotation over one of your imported photos. Choose Sans, Serif or Handwritten, font size, text colour and Auto/Top/Middle/Bottom placement. The built-in text bank contains seven original lines per genre, so it repeats weekly and makes no invented author attributions. Photos also advance daily. These photos are not uploaded to an image service.

Auto placement uses ML Kit object detection to identify up to five subject rectangles, then selects a text region with low subject overlap and visual texture. The detector needs an initial model/library download and may be unavailable on devices without the required Google components. Once ready, object processing happens on-device. If detection fails or no subject is recognized, texture-based placement is used. Auto placement is best effort; manual placement is available. A dark text panel improves readability. Crop and text placement are recomputed for the wallpaper's aspect ratio.

### Year dots

Select the dot colour and Dark/Light background, then set the wallpaper. The applied wallpaper contains only the dots and a small bottom footer in this format:

`70D left : 81%`

The first number is days remaining after today's dot fills; the percentage is **year completed**, rounded to the nearest whole percent. The earlier mockup's `70D left : 85%` was illustrative: 70 days remaining in a 365-day year is about 81% completed. There are 365 dots in ordinary years and 366 in leap years. There is no year label, date, legend or day count on the applied wallpaper. Controls appear only in the app.

### Streak widgets

Open **Streak widgets**, enter a habit name, choose an accent colour and Compact/Wide, and tap **Add widget**. Confirm your launcher's pin request. Repeat for Study, Gym, Reading or any other habits. If pinning is unsupported, long-press the home screen → Widgets → **PIXLO Streak**, drag it onto the home screen, and configure it. Each added widget has its own data.

- First daily tap: 🔥 1.
- Tap on the next calendar day: 🔥 2.
- Additional taps toggle today's check-in: first day **0 ↔ 1**, second consecutive day **1 ↔ 2**. They never award extra days. Tapping again undoes only today's check-in and restores the exact earlier streak state.
- Miss an entire day: that widget displays 🔥 0; its next tap restarts at 1.

App **Edit** controls change existing widget appearance/name without resetting its count. Resize through the launcher and remove a widget from the home screen when no longer needed. Removed widgets' data is deleted. Widget visual reset uses date/time broadcasts, the launcher update cycle and a periodic background refresh. Android can delay those while asleep or battery-restricted; tap logic always recalculates against the current local date. A backward date change restarts on the next tap rather than awarding extra days.

## Free browser workflow

1. Create a public [GitHub](https://github.com) repository. Standard GitHub-hosted Actions runners are [free for public repositories](https://docs.github.com/en/billing/concepts/product-billing/github-actions). Private repositories have plan allowances.
2. Extract the source ZIP and upload the **contents** of its `PIXLO` directory. `app`, `gradle`, `settings.gradle.kts` and `.github` must be at the repository root. Include `.github/workflows/android.yml`, `.gitignore`, and the wrapper JAR. An empty repository may show an **upload existing file** link instead of **Add file → Upload files**.
3. The commit starts **Actions → Build PIXLO APK**. It runs 12 JVM tests, lint, app compilation and device-test APK compilation. After success, download **PIXLO-debug-APK** from that run and extract `app-debug.apk`.
4. Edit files with the pencil or press `.` to open GitHub's browser editor. Commit to rebuild. You do not need Codespaces.
5. Optional: **Run workflow → device_tests** runs Android rendering and widget-state persistence tests in a cloud emulator. Physical launcher integration still needs phone checks.

Artifacts require signing in and expire after 14 days. Separate cloud builds may use different temporary debug signing keys; a signature mismatch means uninstall/reinstall. This is a debug app for personal sideload testing, not a Play Store release.

## Phone test checklist

- Update the old local APK; confirm previously imported photos remain.
- Import landscape, portrait and rotated photos. Try every filter, block size and transition. Confirm the preview and home-screen wallpaper agree.
- With 2+ photos, test 10/30/60 and a 17-second custom interval. Pause/resume rotation. Hide the wallpaper and return.
- Download each internet genre; verify image source links and attribution. Turn off network and confirm cached images still display. A failed refresh must preserve the old cache.
- Use personal-photo quotes: test all genres, fonts, colours and placements; allow initial detector download. Check that faces/subjects remain legible and choose manual placement if needed.
- Check year dots: no extra wallpaper labels, correct daily filling, footer date calculations, dark/light background and colour.
- Add two differently named/coloured widgets. Repeated taps must alternate 0/1 on the first day and 1/2 on the second consecutive day, independently for each widget. Reopen the app or restart the phone and verify undo still restores the previous state. Skip a full day and confirm a new check-in starts at 1; undo must restore the missed-day state. Editing appearance must preserve count.
- Rotate the phone, reopen the app, reboot, change timezone, test large text/small screen, and check your launcher's lock-screen behavior.

## Persistence and privacy

Imported photos are orientation-normalized JPEG copies, maximum 1280 px on the longest edge, in app-private storage; originals are untouched. Transparent/animated source images become JPEG stills. Settings, cached internet images, object rectangles and each widget's state are persisted. Clearing data/uninstalling removes these copies. Backup/device transfer of app-private data is disabled.

Internet access is used for Commons downloads and the initial ML Kit setup. Network state supports constrained background refresh; boot permission is used by WorkManager to restore scheduling. There is no broad photo/storage permission, login, advertising or cloud photo upload. No exact-alarm permission is used.

## Build and code

Java 17, Gradle 8.9, AGP 8.7.3, Kotlin/Compose 2.0.21, Android SDK/build-tools 35. Run `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` with Java 17 and an Android command-line SDK, or use the browser workflow. Android Studio is optional.

`MainActivity.kt`: UI; `PhotoStore.kt`: imports/settings; `PhotoFilters.kt`: filters; `WallpaperArt.kt`: dots/quote composition; `SubjectDetection.kt`: on-device subject boxes; `InternetQuotes.kt`: online cache/attribution; `PixloWallpaperService.kt`: visibility-aware drawing/transitions; `DailyRules.kt`: testable date/streak rules; `StreakWidgets.kt` and `WidgetConfigActivity.kt`: multiple widgets; `DailyRefresh.kt`: background refresh.

See [VALIDATION.md](VALIDATION.md) for actual local build results and limits. The build workflow is published in this repository; check the Actions tab for each cloud run's status.

Sources: [ML Kit object detection](https://developers.google.com/ml-kit/vision/object-detection/android), [Android widget pinning](https://developer.android.com/develop/ui/views/appwidgets/discoverability), [Commons API](https://commons.wikimedia.org/wiki/Commons:API/MediaWiki).
