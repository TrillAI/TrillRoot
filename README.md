# Trill Root

**A modern, offline root-readiness assistant for Android devices you control.**

Trill Root is a native Android app that explains legitimate rooting routes and helps you prepare for them. It is **not** a universal one-click root tool, an exploit collection, or a bootloader-lock bypass.

## Download the preview

The built preview is [`artifacts/TrillRoot-0.2.0-debug.apk`](artifacts/TrillRoot-0.2.0-debug.apk).

- Android **6.0 and newer** (minimum API 23).
- Compiled for and targeting **Android 16 / API 36**.
- No native libraries: the same APK contains bytecode for ARM and x86 devices.
- No runtime dependencies, advertising, analytics, or Android permissions.
- **Debug-signed for evaluation and sideloading**, not a production/store release.

Android may require you to allow installs from the particular browser or file manager you use. Review that prompt and disable the permission afterward. Do not replace an existing root manager with this app; it is a separate assistant.

## What works

- **First-run safety flow:** branded fades, scroll-gated Terms of Use, a no-permissions disclosure, and ten transparent identification status updates.
- **Auto-detected readiness:** an explainable conservative verdict based on reported unlock and bootloader signals; unknown remains unknown.
- **Neon AMOLED interface:** true-black surfaces, bright multicolor headings, and a detailed shield/circuit icon.
- **Device snapshot:** model, codename, Android/API level, security patch, build fingerprint, CPU ABI, and optional bootloader/partition properties.
- **Conservative interpretation:** absent or conflicting lock signals remain unconfirmed. Verified-boot color is not treated as a definitive lock state.
- **Optional existing-root check:** after explicit confirmation, runs only `su -c id`, with a 20-second deadline and bounded output. A grant is not persisted as a permanent device verdict.
- **Multiple documented routes:** Magisk image patching, Samsung-specific firmware patching, KernelSU compatibility review, and deprecated legacy-recovery guidance.
- **Preparation checklist:** eight locally saved acknowledgements covering authorization, backups, exact firmware, recovery, and security trade-offs.
- **Readiness report:** exports a text file only through Android's document picker, to a destination you choose.
- **Official documentation links:** open externally in your browser; the app itself has no internet permission.
- **Native interface:** scrolling layouts, large touch targets, tablet width limits, system-bar insets, and Android 13+ back navigation.

## What it deliberately does not do

Trill Root does **not** install `su`, bundle Magisk/KernelSU, exploit vulnerabilities, unlock a bootloader, flash an image, disable verified boot, change SELinux, or bypass FRP/carrier/management restrictions. It never tries a different elevation method after a failed root check.

There is no honest “works on every Android version” guarantee. An officially unsupported or policy-locked device may have no supported rooting route. Android version alone cannot establish bootloader eligibility, ramdisk layout, kernel compatibility, or the correct partition to patch.

## Choose a route

| Situation | Route to evaluate | Important limit |
|---|---|---|
| Already granted UID 0 | Review the existing root manager | Do not stack another root solution blindly |
| Officially unlockable non-Samsung device | Magisk image patching | Exact firmware and model-specific flashing procedure required |
| Unlockable Samsung variant | Samsung-specific Magisk guide | Download Mode workflow; data loss and permanent Knox consequences |
| Explicitly supported device/kernel | KernelSU | A GKI label or matching Android version is not enough |
| Older, maintained device | Legacy recovery documentation | Deprecated; never flash an arbitrary old package |
| Carrier/policy-locked device | Stop and consult the manufacturer/OEM | No bypass is provided |

Read the [safety and scope notes](docs/SAFETY.md) before modifying a device.

## Build

Requirements: a full **JDK 17+**, Android **platform 36**, and **build-tools 36.1.0**. Python 3.10+ is needed for the direct build script. The JVM regression tests use a POSIX shell (`/bin/sh`).

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

python3 tools/build_apk.py
```

Explicit paths also work:

```sh
python3 tools/build_apk.py \
  --java-home /path/to/jdk-17 \
  --sdk /path/to/android-sdk
```

The script runs the JVM tests, compiles resources and Java, creates DEX, aligns the APK, debug-signs it, verifies its signatures, and checks that it declares no permissions. Output goes to the ignored `artifacts/` directory. The debug keystore stays in ignored `.local-signing/`.

A conventional Gradle project is also included:

```sh
./gradlew assembleDebug
```

Use Gradle 8.13 and AGP 8.11.1. The wrapper distribution checksum is pinned. The delivered preview was built with the direct SDK script; see [BUILDING.md](docs/BUILDING.md) for release signing and verification details.

## Verification

The preview build passed:

- 129 JVM regression checks.
- APK Signature Scheme v1, v2, and v3 verification.
- ZIP alignment and 16 KiB native-library page-alignment checks (there are no native libraries).
- Manifest checks: minimum API 23, target API 36, no declared permissions.

**No physical-device or emulator runtime test has been performed.** Signing and compilation are not proof of compatibility with a particular phone. Follow the [device validation checklist](docs/DEVICE_VALIDATION.md) before calling a build production-ready.

## Source layout

```text
app/src/main/java/com/trillai/trillroot/
  core/       Pure Java signal interpretation, route catalog, checklist, reports
  device/     Fixed-command runner and non-mutating device collector
  ui/         Native Android interface
app/src/test/ Dependency-free JVM regression tests
app/src/main/res/
tools/        Direct, offline SDK build script
docs/         Safety, build, and device-validation notes
```

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for the trust boundaries.

## Primary references

- [Android bootloader locking/unlocking](https://source.android.com/docs/core/architecture/bootloader/locking_unlocking)
- [Magisk installation guide](https://github.com/topjohnwu/Magisk/blob/master/docs/install.md)
- [KernelSU installation guide](https://kernelsu.org/guide/installation.html)

Trill Root is independent and is not affiliated with these projects, Samsung, Google, or KingRoot. Original app code is MIT-licensed; the Gradle wrapper retains its upstream Apache-2.0 license.
