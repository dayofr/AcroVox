# AcroVox

Android podcast player, inspired by AntennaPod. Built with AI assistance.

> Install and update via Obtainium: see [OBTAINIUM.md](OBTAINIUM.md).

## Screenshots

| | | |
|---|---|---|
| ![Home](fastlane/metadata/android/en-US/images/phoneScreenshots/1_home.png) | ![Player](fastlane/metadata/android/en-US/images/phoneScreenshots/2_player.png) | ![Queue](fastlane/metadata/android/en-US/images/phoneScreenshots/3_queue.png) |
| ![Library](fastlane/metadata/android/en-US/images/phoneScreenshots/4_library.png) | ![Downloads](fastlane/metadata/android/en-US/images/phoneScreenshots/5_downloads.png) | ![Inbox](fastlane/metadata/android/en-US/images/phoneScreenshots/6_inbox.png) |

## Prerequisites

- JDK 17 or later (`brew install openjdk@17`).
- Android SDK with platform 37.
- `local.properties` with `sdk.dir=...` (not versioned).

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew assembleDebug
```
