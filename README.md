# Material You Music (Android)

An offline-first Android music player project with a Material You-inspired interface.

## Features
- Native Android app, minimum Android 6.0 (API 23)
- Select local audio using Android's system file picker
- Playlist, play/pause, next/previous, seek, volume, shuffle, repeat-one
- Light and dark themes
- No internet permission and no ads

## Build an APK
1. Install Android Studio on a computer.
2. Open this project folder in Android Studio.
3. Allow Gradle sync to download the Android build tools and dependencies.
4. Select **Build > Build APK(s)**.
5. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Important
This source project is not itself an APK. It needs to be built with Android Studio or Gradle before installation. Test the built APK on your device. This starter version focuses on local playback while the app is open; it does not yet include a media notification / full background playback service.
