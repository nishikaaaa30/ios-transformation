# iOS Transformation (Android launcher starter)

An Android Studio project for a local-first, iOS-inspired home launcher. It is an independent Android customization app and is not affiliated with Apple.

## Current implementation

- Declares itself as both a normal app and an Android HOME launcher.
- Home screen with installed launcher app icons, a four-app dock when common apps are installed, date/time, and shortcut tiles.
- App Library with search and launch actions.
- Personalization dashboard with dark appearance and locally saved accent color.
- Import a photo using Android's system photo picker; preview it in the launcher and optionally set it as system wallpaper.
- Control Center-style panel whose controls open the real Android settings pages. It deliberately does not display fake on/off states.
- Uses the Android package manager to enumerate launchable apps; no `QUERY_ALL_PACKAGES` permission is requested.
- No account, analytics, network access, or photo uploads.

## Build

Requirements:
- Android Studio Ladybug or newer (or compatible Android Gradle Plugin 8.7.3)
- JDK 17
- Android SDK Platform 35 installed through Android Studio SDK Manager
- Internet access for Gradle to download dependencies on the first build

Steps:
1. Unzip this project.
2. Open the `ios-transformation` folder in Android Studio.
3. Allow Gradle sync to finish and install missing SDK components if prompted.
4. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK will normally be created at `app/build/outputs/apk/debug/app-debug.apk`.
6. Install the APK on the Galaxy phone (USB debugging / `adb install` or transfer the APK and open it). Android may ask you to allow installation from that source.
7. Open **Settings > Apps > Choose default apps > Home app** (wording varies by One UI version) and choose **iOS Transformation**. The dashboard also opens Android's Home settings.

To build from a terminal after installing Gradle and the Android SDK:

```bash
gradle assembleDebug
```

This project intentionally does not include a Gradle wrapper binary. The current generation environment did not have Android SDK/Gradle available, so an APK could not be compiled or verified here. Build it in Android Studio using the steps above.

## Samsung notes

- Menu names vary across One UI releases. Search Settings for “Default apps” or “Home app” if needed.
- To restore the Samsung launcher, return to **Settings > Apps > Choose default apps > Home app** and select **One UI Home**.
- If icons or app labels are missing, update Android System WebView / One UI Home and restart the launcher. Some work-profile or protected apps may not expose a launchable activity.
- The selected photo is held in memory for the preview during the current app session. Applying it uses Android's `WallpaperManager`; it does not change the lock-screen security configuration.
- Android 15 edge-to-edge behavior and OEM power management can vary by device. Test on your exact Galaxy model before distributing broadly.

## Features not implemented in this starter

This is a functional launcher foundation, not a full clone of iOS. The dock currently auto-selects up to four common apps rather than offering a drag-and-drop dock editor. Android widgets, folders, multi-page drag/reorder, icon packs, live weather, music/charging Dynamic Island animations, notification previews, and a custom lock screen are not yet implemented. Real system lock-screen replacement and secure authentication are intentionally avoided. Notification access, overlay access, and broad device-control permissions are not requested. The Control Center opens actual system settings instead of pretending restricted toggles were changed.

## Permissions and privacy

- `SET_WALLPAPER`: needed only when the user taps Apply wallpaper.
- `<queries>` for launcher activities: lets the app discover launchable apps without requesting broad `QUERY_ALL_PACKAGES` access.
- The system photo picker is used to select a photo. The app does not upload it.

## Project structure

```text
app/src/main/AndroidManifest.xml
app/src/main/java/com/example/iostransformation/MainActivity.kt
app/build.gradle.kts
build.gradle.kts
settings.gradle.kts
gradle.properties
```

## Build APK online without installing Android Studio on your laptop

This project includes a GitHub Actions workflow at `.github/workflows/build-apk.yml`.

1. In a web browser, sign in to GitHub and create a new repository.
2. On your laptop, extract this ZIP using Windows' built-in **Extract all** option.
3. In the new GitHub repository, choose **Add file > Upload files** and upload the extracted project contents, including the hidden `.github/workflows/build-apk.yml` file. Make sure the workflow is present in the repository at `.github/workflows/build-apk.yml`.
4. Open the repository's **Actions** tab, choose **Build Android APK**, and click **Run workflow**.
5. When the workflow finishes successfully, open that run and download the `ios-transformation-debug-apk` artifact. GitHub may require you to sign in and download the artifact ZIP.
6. Extract the artifact ZIP; it contains `app-debug.apk`. Transfer that APK to your Galaxy phone and tap it to install. If prompted, allow installation from the file/browser app you used, then turn that permission off again afterward if desired.

The online build needs a GitHub account and internet access. The APK is a debug build, suitable for personal testing; it is not a Play Store release. The workflow has not been run from this environment, so its result still needs to be verified by a successful GitHub Actions run.
