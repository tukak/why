# Why?

An Android app that asks one question every time you unlock your phone: *what brings you here?*

Tap a reason, tap "Just habit", or type your own. Over the days you see what really pulls you to your phone, and when.

## Features

- A question after each unlock, with your reasons sorted for the time of day
- Today's jar: every unlock drops a pebble in. Tilt the phone and the pebbles roll.
- Week view: daily jars, where the time went, and a heat map of habit unlocks
- A check-in when a session runs long (can be turned off per reason)
- An evening summary notification
- Answers you type often can become reasons with one tap

## Privacy

- Everything stays on the phone. The app has no internet permission.
- No accounts, no ads, no analytics.
- It does not read which apps you use. It only notices unlocks and screen off.

## Permissions

| Permission | Why |
|---|---|
| Display over other apps | To show the question on top of whatever you open |
| Notifications | Android needs a notification to keep the app running |
| Battery: Unrestricted (recommended) | So the system does not stop the app in the background |

## Build

Requirements: JDK 17 or newer (the one bundled with Android Studio works), Android SDK with API 37.

```sh
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # unit tests
./gradlew lintDebug
```

Minimum Android version: 12 (API 31).

Release: `./release.sh 1.2.3 "What changed"` sets the version, writes the English changelog, runs tests and lint, commits and tags `v1.2.3`, then builds the signed APK and AAB. Builds are reproducible: F-Droid builds the same tag and ships it with the upstream signature. Run it from a normal clone (not a git worktree), because the APK records the commit.

## Languages

English, Czech, Slovak, German, Spanish, Brazilian Portuguese, French and Polish. On Android 13 and newer you can pick the app language in system settings (Apps → Why? → Language).

Corrections from native speakers are welcome: the texts live in `app/src/main/res/values-*/strings.xml`, store texts in `fastlane/metadata/android/`.

## Support

The app is free. If it helps you, you can [buy me a coffee on Ko-fi](https://ko-fi.com/tukak).

## License

[GPL-3.0](LICENSE). Fonts: Bricolage Grotesque and Figtree, under the SIL Open Font License (`app/src/main/assets/licenses/`).
