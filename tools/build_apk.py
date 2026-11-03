#!/usr/bin/env python3
"""Build TrillRoot with an installed Android SDK, without downloading dependencies.

Requires JDK 17+, Android platform 36, and build-tools 36.1.0. The default APK
is debug-signed for sideloading. Release signing material must stay outside Git.
"""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
PACKAGE = "com.trillai.trillroot"


def run(args: list[str], env: dict[str, str] | None = None, capture: bool = False) -> str:
    printable = " ".join(str(a) for a in args)
    print("+ " + printable, flush=True)
    result = subprocess.run(args, cwd=ROOT, env=env, text=True,
                            stdout=subprocess.PIPE if capture else None,
                            stderr=subprocess.STDOUT if capture else None)
    if result.returncode:
        if capture:
            print(result.stdout)
        raise SystemExit(result.returncode)
    return result.stdout if capture else ""


def java_tool(home: Path, name: str) -> str:
    executable = home / "bin" / (name + (".exe" if os.name == "nt" else ""))
    if not executable.is_file():
        raise SystemExit(f"Missing {executable}. Set --java-home or JAVA_HOME to a full JDK.")
    return str(executable)


def config_value(name: str) -> str:
    text = (APP / "build.gradle").read_text()
    match = re.search(rf"\b{name}\s+['\"]?([^'\"\s]+)", text)
    if not match:
        raise SystemExit(f"Cannot read {name} from app/build.gradle")
    return match.group(1)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, default=os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT"))
    parser.add_argument("--java-home", type=Path, default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--release", action="store_true", help="Sign with TRILLROOT_KEYSTORE and password environment variables")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if not args.sdk or not args.java_home:
        raise SystemExit("Set ANDROID_HOME and JAVA_HOME, or pass --sdk and --java-home.")
    sdk, java_home = args.sdk.resolve(), args.java_home.resolve()
    version = config_value("versionName")
    min_sdk = config_value("minSdk")
    target_sdk = config_value("targetSdk")
    compile_sdk = config_value("compileSdk")
    version_code = config_value("versionCode")
    platform = sdk / "platforms" / f"android-{compile_sdk}" / "android.jar"
    tools = sdk / "build-tools" / config_value("buildToolsVersion")
    for path in [platform, tools / "aapt2", tools / "d8", tools / "apksigner", tools / "zipalign"]:
        if not path.exists():
            raise SystemExit(f"Missing SDK component: {path}")
    env = os.environ.copy()
    env["JAVA_HOME"] = str(java_home)
    env["PATH"] = str(java_home / "bin") + os.pathsep + env.get("PATH", "")
    javac, java = java_tool(java_home, "javac"), java_tool(java_home, "java")
    keytool = java_tool(java_home, "keytool")
    aapt2, d8 = str(tools / "aapt2"), str(tools / "d8")
    apksigner, zipalign = str(tools / "apksigner"), str(tools / "zipalign")
    build = APP / "build" / "direct"
    if build.exists():
        shutil.rmtree(build)
    build.mkdir(parents=True)
    classes, tests, dex, generated = [build / n for n in ("classes", "tests", "dex", "generated")]
    for directory in (classes, tests, dex, generated):
        directory.mkdir()

    # The app core and process runner are Android-independent and tested on the JVM.
    core_sources = sorted((APP / "src/main/java/com/trillai/trillroot/core").glob("*.java"))
    runner_source = APP / "src/main/java/com/trillai/trillroot/device/CommandRunner.java"
    test_source = APP / "src/test/java/com/trillai/trillroot/core/CoreTest.java"
    run([javac, "-Xlint:all", "-d", str(tests), *map(str, core_sources + [runner_source, test_source])])
    run([java, "-ea", "-cp", str(tests), "com.trillai.trillroot.core.CoreTest"])

    # Keep the source manifest namespace-free for modern AGP; inject it for aapt2.
    manifest = ET.parse(APP / "src/main/AndroidManifest.xml")
    manifest.getroot().set("package", PACKAGE)
    processed_manifest = build / "AndroidManifest.xml"
    ET.register_namespace("android", "http://schemas.android.com/apk/res/android")
    manifest.write(processed_manifest, encoding="utf-8", xml_declaration=True)
    compiled_resources = build / "resources.zip"
    run([aapt2, "compile", "--dir", str(APP / "src/main/res"), "-o", str(compiled_resources)])
    unsigned = build / "unsigned.apk"
    run([aapt2, "link", "-I", str(platform), "--manifest", str(processed_manifest),
         "--java", str(generated), "--min-sdk-version", min_sdk,
         "--target-sdk-version", target_sdk, "--version-code", version_code,
         "--version-name", version, "-o", str(unsigned), str(compiled_resources)])
    sources = sorted((APP / "src/main/java").rglob("*.java")) + sorted(generated.rglob("*.java"))
    run([javac, "-Xlint:all,-options", "-source", "8", "-target", "8",
         "-classpath", str(platform), "-d", str(classes), *map(str, sources)])
    class_files = sorted(classes.rglob("*.class"))
    run([d8, "--release", "--min-api", min_sdk, "--lib", str(platform),
         "--output", str(dex), *map(str, class_files)], env=env)

    # Preserve aapt2's resource compression/alignment; add only the compiled DEX.
    with zipfile.ZipFile(unsigned, "a") as archive:
        archive.write(dex / "classes.dex", "classes.dex", compress_type=zipfile.ZIP_DEFLATED)
    aligned = build / "aligned.apk"
    run([zipalign, "-f", "-P", "16", "4", str(unsigned), str(aligned)])
    kind = "release" if args.release else "debug"
    output = args.output or ROOT / "artifacts" / f"TrillRoot-{version}-{kind}.apk"
    output = output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    if args.release:
        keystore = os.environ.get("TRILLROOT_KEYSTORE")
        alias = os.environ.get("TRILLROOT_KEY_ALIAS")
        if not keystore or not alias or not Path(keystore).is_file():
            raise SystemExit("Release builds require TRILLROOT_KEYSTORE and TRILLROOT_KEY_ALIAS.")
        if not os.environ.get("TRILLROOT_KEYSTORE_PASSWORD") or not os.environ.get("TRILLROOT_KEY_PASSWORD"):
            raise SystemExit("Set TRILLROOT_KEYSTORE_PASSWORD and TRILLROOT_KEY_PASSWORD; never put them in source control.")
        sign = [apksigner, "sign", "--ks", keystore, "--ks-key-alias", alias,
                "--ks-pass", "env:TRILLROOT_KEYSTORE_PASSWORD", "--key-pass", "env:TRILLROOT_KEY_PASSWORD"]
    else:
        keystore_path = ROOT / ".local-signing" / "debug.keystore"
        keystore_path.parent.mkdir(exist_ok=True)
        if not keystore_path.exists():
            run([keytool, "-genkeypair", "-keystore", str(keystore_path), "-storetype", "PKCS12",
                 "-storepass", "android", "-keypass", "android", "-alias", "androiddebugkey",
                 "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
                 "-dname", "CN=Android Debug,O=TrillRoot,C=US"])
        sign = [apksigner, "sign", "--ks", str(keystore_path), "--ks-key-alias", "androiddebugkey",
                "--ks-pass", "pass:android", "--key-pass", "pass:android"]
    run(sign + ["--v1-signing-enabled", "true", "--v2-signing-enabled", "true",
                "--v3-signing-enabled", "true", "--v4-signing-enabled", "false",
                "--out", str(output), str(aligned)], env=env)
    verification = run([apksigner, "verify", "--verbose", "--print-certs", str(output)], env=env, capture=True)
    alignment = run([zipalign, "-c", "-P", "16", "4", str(output)], capture=True)
    badging = run([str(tools / "aapt"), "dump", "badging", str(output)], capture=True)
    permissions = run([str(tools / "aapt"), "dump", "permissions", str(output)], capture=True)
    if "uses-permission:" in permissions:
        raise SystemExit("Unexpected permission in a permission-free application")
    if f"targetSdkVersion:'{target_sdk}'" not in badging or f"sdkVersion:'{min_sdk}'" not in badging:
        raise SystemExit("APK SDK versions do not match the build configuration")
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    (output.parent / (output.name + ".sha256")).write_text(f"{digest}  {output.name}\n")
    report = (f"TrillRoot {version} ({kind})\nAPK: {output.name}\nSHA-256: {digest}\n"
              f"Compile SDK: {compile_sdk}\nMinimum SDK: {min_sdk}\nTarget SDK: {target_sdk}\n"
              "JVM regression tests: passed\nAPK signature verification: passed\n"
              "ZIP/page alignment verification: passed\nDeclared permissions: none\n"
              "Physical-device testing: not performed by this build script\n\n"
              + verification + "\n" + badging)
    (output.parent / (output.stem + "-verification.txt")).write_text(report)
    print(verification)
    print(f"\nBuilt {output}\nSHA-256: {digest}")
    if not args.release:
        print("DEBUG-SIGNED PREVIEW: suitable for sideloading, not a production release signature.")


if __name__ == "__main__":
    main()
