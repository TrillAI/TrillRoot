# Device validation checklist

The preview has passed compilation, JVM regression tests, package signature verification, and alignment checks. **It has not been run on a physical Android device or emulator.** Complete this matrix before distributing a production build.

Record the exact model, carrier/region, firmware fingerprint, Android version, and test date. Do not record serial numbers, IMEI, accounts, or other unique identifiers in public reports.

## Runtime matrix

Test at least:

- Android 6 or 7 / API 23–25: legacy theme/back behavior and D8 compatibility.
- Android 10 / API 29: scoped-storage document export.
- Android 12 / API 31: splash icon and system bars.
- Android 13+ / API 33+: predictive/back callback behavior and notification-free operation.
- Android 16 / API 36: enforced edge-to-edge, insets, gestures, and current platform behavior.
- One Samsung and one non-Samsung device for route ordering; this does not require modifying either device.

## Functional checks

- [ ] Cold launch completes without requesting root.
- [ ] Device snapshot matches Settings and `adb shell getprop` for exposed fields.
- [ ] Inaccessible properties display as unknown, not locked/unlocked.
- [ ] Conflicting property signals remain unconfirmed (covered by JVM tests).
- [ ] Root-check cancellation makes no `su` request.
- [ ] Confirming the check on a non-root device produces an honest unavailable/not-granted result.
- [ ] On a controlled, already-rooted test device, denying the manager prompt does not trigger another method.
- [ ] Granting the read-only check displays UID 0 confirmation without changing partitions or root configuration.
- [ ] An unanswered prompt reaches the timeout and leaves the UI responsive.
- [ ] Rotate/background the app during collection and root checking; no stale activity update or crash occurs.
- [ ] All four tabs, every route detail, official links, and copy-command buttons work.
- [ ] Checklist state survives app restart and reset affects only app preferences.
- [ ] Export cancellation writes nothing; selecting a destination writes the expected report.
- [ ] Airplane-mode operation works for every in-app feature; external browser links are the only online action.
- [ ] TalkBack, large font sizes, landscape, small screens, and tablet width limits remain usable.
- [ ] Gesture and three-button navigation do not obscure content or bottom tabs.
- [ ] No unexpected permission, service, network connection, or persistent elevated process appears.

## Modification testing is separate

Do not unlock, flash, or root a device merely to test this assistant. Any subsequent validation of a documented rooting workflow requires an authorized, recoverable test device, exact matching firmware, and the relevant upstream project's instructions. Those modifications are outside TrillRoot's execution boundary.

## Report results

For each failure, include the screen/action, expected result, actual result, Android version/API, and a redacted log. Never include a root grant token, account credential, or unique hardware identifier.
