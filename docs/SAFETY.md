# Safety and scope

## Authorized devices only

Use TrillRoot only on a device you own or are explicitly authorized to assess. The app is a readiness assistant; it does not obtain authorization or bypass device protections.

## No universal rooting claim

Modern rooting depends on the device maker's unlock policy, exact model/region/carrier, firmware build, boot-chain configuration, and sometimes exact kernel support. A universal APK cannot legitimately guarantee root across all of those combinations.

TrillRoot compares documented alternatives. It does not “try exploits until one works.” Such a strategy would be unreliable and could compromise the device or its data.

## Elevated-command boundary

There is exactly one elevated command definition:

```text
su -c id
```

It is executed only after the user confirms the root-check dialog. It asks an **already installed** root manager for permission to read the process identity. It does not install a root manager or root binary.

The runner:

- uses a fixed argument vector, not a user-supplied shell string;
- closes standard input;
- bounds retained output;
- enforces a deadline;
- stops on denial, absence, error, or timeout;
- does not retry with another elevation technique.

UID 0 confirms that the command received a root grant. It does not certify the root manager's integrity. A failed check does not prove that the device is unrooted.

## Bootloader information is not attestation

`ro.boot.*` and OEM-unlock properties are optional, vendor-controlled signals. They may be missing, inaccessible, inconsistent, or inappropriate to interpret on a particular device.

- Conflicting lock properties produce **Unconfirmed**.
- An orange verified-boot state alone is not used to declare the bootloader unlocked.
- Reported OEM-unlock support is not a grant of unlock eligibility for a particular unit.
- A slot suffix does not identify which image a root project should patch.
- Launch API does not override the root project's device-specific image guidance.

Verify the bootloader screen and the manufacturer's instructions before changing anything.

## Modification risks

Official unlocking normally erases user data. Rooting or modifying boot images can affect warranty, OTA updates, hardware-backed attestation, banking/enterprise apps, DRM, and recovery. Samsung Knox consequences can be permanent.

Never flash another device's patched image. Never guess between `boot`, `init_boot`, `vendor_boot`, and `recovery` based only on Android version. Never disable AVB/dm-verity as a generic troubleshooting step. Never relock a bootloader while modified images remain installed.

Back up both data and exact stock recovery images. Carrier locks, FRP, KnoxGuard, and enterprise management are stop conditions, not obstacles for this app to defeat.

## Privacy

The app declares no permissions and contains no analytics, advertising, network client, or WebView. It does not request serial number, IMEI, accounts, contacts, or location. Only an allowlist of boot/build properties is retained.

Checklist state is stored in private app preferences; application backup is disabled. A readiness report contains model and build information and is exported only when the user chooses a destination in the system document picker. Review it before sharing.

Official links open in an external browser. That browser has its own network access and privacy policy.
