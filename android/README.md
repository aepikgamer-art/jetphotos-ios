# JetPhotos for Android

Native Android companion to the JetPhotos iOS app. This project is intentionally photo-only: it searches JetPhotos aircraft photos through jetapi.dev, displays responsive photo grids, and supports photo details, saving, sharing, and opening the original JetPhotos source.

## Adaptive layout

- Phones use a compact search header and a two-column adaptive gallery.
- Small tablets increase the gallery column count.
- Large tablets use a persistent search rail, a wider multi-column gallery, and a side-by-side photo details pane.
- The layout responds to available window width rather than assuming a device model.

## API behavior

Uses the same JetAPI endpoint and parameters as iOS:

`https://www.jetapi.dev/api?reg=<registration-or-aircraft-code>&photos=12&only_jp=true`

The response is decoded from the root-level `Reg` and `Images` fields. Search terms must be a registration or a single aircraft code without spaces (for example, `N787BK` or `A350-1000`). Multiword free-text searches are not supported by this API.

## Build

Requirements: Android Studio with JDK 17 and Android SDK 35.

Open the `android/` directory in Android Studio and let Gradle sync, then run the `app` configuration on an Android device or emulator.

Package: `dev.jetphotos.android`

## Notes

- Saving uses Android MediaStore and writes to `Pictures/JetPhotos`.
- Sharing sends the photo's JetPhotos source URL and aircraft details.
- Photo credit and source links remain available in the detail view.
