# JetPhotos

A focused iOS app for searching, viewing, saving, and sharing aircraft photos from JetPhotos through [jetapi.dev](https://www.jetapi.dev).

## Features

- Search by aircraft registration (for example `N142UW`) or aircraft type (`A350`).
- Browse photo results in a responsive grid.
- Open full-size photos and view their aircraft and photographer details.
- Save photos to your photo library, share them, or open the original JetPhotos source.
- Native SwiftUI navigation. On iOS 26, the system tab bar uses Apple's real Liquid Glass design; on iOS 18–25, it uses the system tab bar for that OS version.
- No live map, flight tracking, airport lookup, or FlightRadar integration.

## Install

- **LiveContainer:** [Add Jetphotos source](livecontainer://source?url=https%3A%2F%2Fraw.githubusercontent.com%2Faepikgamer-art%2Fjetphotos-ios%2Fmain%2Faltstore-source.json) or add this catalog URL manually:
  `https://raw.githubusercontent.com/aepikgamer-art/jetphotos-ios/main/altstore-source.json`
- **AltDirect:** [Open the JetPhotos source in AltDirect](https://altdirect.app/?url=https%3A%2F%2Fraw.githubusercontent.com%2Faepikgamer-art%2Fjetphotos-ios%2Fmain%2Faltstore-source.json)
- **IPA:** [Download the latest JetPhotos.ipa](https://github.com/aepikgamer-art/jetphotos-ios/releases/latest/download/JetPhotos.ipa)

The IPA is unsigned and built by GitHub Actions on each push to `main` and for version tags.

## Build locally

Requires Xcode 16 or later and [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```sh
xcodegen generate
xcodebuild -project JetPhotos.xcodeproj -scheme JetPhotos -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build
```

## Credits

- Photo search API: [jetapi.dev](https://www.jetapi.dev)
- Photo source and photographer credits: [JetPhotos](https://www.jetphotos.com)
- App distribution links: [AltDirect](https://altdirect.app)
