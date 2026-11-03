# Architecture and trust boundaries

## Native Android, no runtime dependencies

The app uses Java and Android framework views. It has one launcher activity, no services/receivers/content providers, no native code, no WebView, and no network permission. D8 supplies compatibility for Java-language features on the minimum API level.

## Core

`core/` is Android-independent:

- `DeviceInfo`: immutable public build information and allowlisted properties.
- `PropertySignals`: conservative lock/support/slot interpretation; unknown and conflicting values stay unknown.
- `RootAccess`: interprets the result of the single read-only root probe.
- `RouteEngine`: orders static, documented alternatives by vendor and existing access. It does not claim exploit availability or exact-device support.
- `Checklist`: preparation items, independent of any flashing operation.
- `ReportFormatter`: explicit user-initiated text export with limitations.

Keeping these classes free of Android imports allows fast JVM regression tests.

## Device boundary

`DeviceCollector` reads public `Build` fields and a fixed list of properties using `/system/bin/getprop`. It never requests arbitrary properties such as serial numbers.

Its separate `probeRoot()` method calls the fixed `su -c id` argument vector. The UI, not startup collection, is responsible for explicit consent.

`CommandRunner` uses `ProcessBuilder` without shell interpolation, closes stdin, bounds retained output, schedules a deadline, and destroys/closes active work on teardown. It is a general internal helper, but the production app gives it only the fixed property and root-check commands. Tests exercise the helper with local POSIX commands.

## UI boundary

`MainActivity` owns four tabs: Overview, Routes, Checklist, and About. Route details remain within the Routes tab. Worker results are posted to the main thread with a lifecycle generation token so a destroyed activity is not updated.

The interface handles modern edge-to-edge insets and registers an Android 13+ back callback, with legacy back handling below API 33. Checklist changes are saved locally. Root grants are not stored as permanent truth.

External documentation uses `ACTION_VIEW` with an HTTPS host allowlist. Export uses `ACTION_CREATE_DOCUMENT`; the app receives only the user-selected destination and needs no broad storage permission.

## Build boundary

The app has both a standard Gradle configuration and a direct SDK build script. The direct script compiles resources, Java, and DEX, then aligns, signs, and verifies the APK. It also runs JVM tests and rejects any declared permission.

Build-time SDK components are not packaged into the app. No root binaries, firmware, kernels, exploit payloads, or downloaded installers are included.
