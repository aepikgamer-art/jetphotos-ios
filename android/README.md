# JetPhotos for Android

Native Android port of the JetPhotos iOS app. This is a focused aircraft-photo browser: it does not include flight tracking, maps, airport lookup, or unrelated features.

## Features

- Search JetAPI by aircraft registration or a single aircraft code (for example `N787BK` or `A350-1000`).
- Responsive aircraft photo gallery using adaptive grid columns.
- Tablet two-pane layout with the gallery beside selected-photo details.
- Full-size photo viewing, aircraft and photographer metadata, save to the device photo library, share, and open the JetPhotos source.
- Choose 6, 12, or 20 photos per search.

## Build

Requirements: Android Studio with JDK 17 and Android SDK 35.

Open the `android` directory in Android Studio, allow Gradle sync, then run the `app` configuration on a phone or tablet.

Command line (Gradle 8.9):

```sh
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## API

Uses `https://www.jetapi.dev/api` with `reg`, `photos`, and `only_jp=true`. The app reads the API's root-level `Reg` and `Images` fields. JetAPI search accepts a registration or a single aircraft code, not multi-word free-text phrases.

Photo credits remain with the photographers and JetPhotos. JetPhotos is an independent app and is not affiliated with JetPhotos.com.
