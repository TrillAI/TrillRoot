# Building and verifying

## Toolchain

- Full JDK 17 or newer.
- Android SDK platform 36.
- Android build-tools 36.1.0.
- Python 3.10+ and POSIX `/bin/sh` for the direct build/test path.
- No third-party runtime libraries are required by the app.

Install SDK components with Android Studio's SDK Manager or an official `sdkmanager` installation. The build scripts do not download SDK components or root binaries.

## Direct SDK build (used for the delivered preview)

```sh
python3 tools/build_apk.py --java-home "$JAVA_HOME" --sdk "$ANDROID_HOME"
```

The script performs these steps:

1. Compile and run the Android-independent JVM regression suite.
2. Compile and link resources with `aapt2`.
3. Compile Java at source/target 8 and dex with `d8 --min-api 23`.
4. Add DEX, align with `zipalign -P 16 4`, and sign with `apksigner`.
5. Verify v1/v2/v3 signatures, alignment, SDK levels, and absence of permissions.
6. Write the APK, SHA-256 sidecar, and verification report to `artifacts/`.

The source manifest is namespace-free for AGP. The direct script injects the package name into a generated build manifest; it does not alter the source manifest.

Debug signing uses a locally generated key in `.local-signing/debug.keystore`. That directory and all keystores are ignored by Git. The debug key is not a production identity. Rebuilds on another machine will have a different signature and may require uninstalling the old preview first.

## Gradle build

```sh
./gradlew assembleDebug
```

The project uses AGP 8.11.1 and Gradle 8.13. The wrapper JAR/scripts come from the upstream Gradle v8.13.0 tag, and the distribution SHA-256 is pinned in `gradle-wrapper.properties`.

Gradle output is normally `app/build/outputs/apk/debug/app-debug.apk`. The delivered artifact was produced by the direct SDK path, not by a completed local Gradle run. CI uses the direct path so the exact delivered build procedure is exercised.

## Production signing

Create and protect your own release key outside the repository. Supply it through environment variables:

```sh
export TRILLROOT_KEYSTORE=/secure/path/release.p12
export TRILLROOT_KEY_ALIAS=your-alias
# Set passwords in your secret manager/shell without committing or printing them.
# TRILLROOT_KEYSTORE_PASSWORD
# TRILLROOT_KEY_PASSWORD

python3 tools/build_apk.py --release
```

The script passes password variable names to `apksigner`, not password values in source code. Release signing does not make the app a universal root tool or replace device validation.

## Independent verification

```sh
"$ANDROID_HOME/build-tools/36.1.0/apksigner" verify --verbose --print-certs \
  artifacts/TrillRoot-0.2.0-debug.apk

"$ANDROID_HOME/build-tools/36.1.0/zipalign" -c -P 16 4 \
  artifacts/TrillRoot-0.2.0-debug.apk

"$ANDROID_HOME/build-tools/36.1.0/aapt" dump permissions \
  artifacts/TrillRoot-0.2.0-debug.apk

sha256sum -c artifacts/TrillRoot-0.2.0-debug.apk.sha256
```

Run the checksum command from the directory containing the APK, or adjust the sidecar path accordingly. The sidecar records the filename, not an absolute build-machine path.

## Preview validation environment

The delivered APK was compiled with Temurin JDK 17 and Android build-tools 36.1.0 against an API 36 platform jar. Because Google's SDK/download hosts were unavailable in the build environment, cached toolchain components were obtained from public GitHub mirrors and checked against the mirror's published SHA-256 manifests. The API 36 jar's package metadata was checked with `aapt2`.

These are build-time inputs only; no mirror code or SDK is bundled in the APK. For a production pipeline, install the SDK through official Google channels and verify your own signing/provenance policy. A mirror checksum establishes consistency with the mirror, not an independent Google signature.
