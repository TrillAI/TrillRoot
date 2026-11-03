package com.trillai.trillroot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Transparent ordering based on vendor and existing access, never on exploit availability. */
public final class RouteEngine {
    public static final String MAGISK_INSTALL = "https://github.com/topjohnwu/Magisk/blob/master/docs/install.md";
    public static final String MAGISK_RELEASES = "https://github.com/topjohnwu/Magisk/releases/latest";
    public static final String KERNELSU_INSTALL = "https://kernelsu.org/guide/installation.html";
    public static final String AOSP_UNLOCK = "https://source.android.com/docs/core/architecture/bootloader/locking_unlocking";

    private RouteEngine() { }

    public static Route existing() {
        return new Route("existing", "Review existing root", "ACCESS REVIEW",
                "Root access is already available to this app. A second rooting method is not needed.",
                "Shown because your root manager returned UID 0.",
                list("Identify the root manager you installed and trust.",
                        "Review its superuser allowlist and remove unwanted grants."),
                list("Open your existing root manager and verify this app's grant.",
                        "Use that manager's official update and uninstall instructions.",
                        "Before an OTA, follow the root project's update-preservation guide.",
                        "Do not install a second root solution over the first."),
                "A UID 0 result does not establish that the root implementation is safe or free of tampering.",
                MAGISK_INSTALL, "Root project documentation", "adb devices");
    }

    public static Route magisk() {
        return new Route("magisk", "Magisk image patching", "DOCUMENTED ROUTE",
                "Patch the correct image from your exact firmware, then follow your device maker's flashing instructions.",
                "Requires an officially unlockable device and a computer. Samsung devices should use the Samsung-specific route instead of generic fastboot steps.",
                list("Official OEM-unlock eligibility for this exact model and carrier.",
                        "A complete backup and a verified stock-firmware recovery plan.",
                        "Firmware matching the installed build; do not match by model name alone.",
                        "Official Magisk app and Android platform-tools on your computer."),
                list("Follow the OEM's unlock procedure. Unlocking normally erases all user data.",
                        "Install the official Magisk app and check its Ramdisk result.",
                        "Use Magisk to patch the correct boot, init_boot, vendor_boot, or recovery image from your exact firmware. Let Magisk and the device documentation determine the target.",
                        "Patch on the device that will run the image. Never use someone else's patched image.",
                        "Transfer the patched output to your computer and follow the model-specific official flashing procedure.",
                        "Boot, complete any Magisk setup, and verify access. Keep the matching stock recovery files."),
                "Do not guess a partition from the Android version. Do not disable AVB/dm-verity as a universal fix. Never relock a bootloader with modified images.",
                MAGISK_INSTALL, "Open Magisk installation guide", "adb shell getprop ro.build.display.id");
    }

    public static Route samsung() {
        return new Route("samsung", "Samsung firmware route", "VENDOR-SPECIFIC",
                "Samsung uses its own unlock and Download Mode workflow. Generic fastboot instructions are not a substitute.",
                "Some carrier and regional variants do not offer bootloader unlocking. Check your exact model before proceeding.",
                list("An officially unlockable Samsung variant; verify OEM Lock in Download Mode.",
                        "Understanding that Samsung Knox changes can be permanent and affect warranty and protected apps.",
                        "The exact official firmware package and current Magisk Samsung instructions.",
                        "A full backup: the documented first-install workflow includes data wipes."),
                list("Check OEM unlocking availability and the device's Download Mode lock status.",
                        "If eligible, follow the current Magisk Samsung unlock procedure, including its physical on-device confirmations.",
                        "Obtain firmware matching your model, region, and required build using an official source.",
                        "Patch the firmware AP package with Magisk on this device as described in the current Samsung guide.",
                        "Use the flashing tool and file combination specified by that guide. First installation and upgrades have different data-preservation requirements.",
                        "Complete the documented reset and Magisk setup; do not substitute generic fastboot commands."),
                "TrillRoot cannot bypass carrier locks, KnoxGuard, FRP, or device-management policy. Do not attempt to defeat them. Stop if your variant is not eligible.",
                MAGISK_INSTALL + "#samsung-devices", "Open Samsung-specific guide", "adb shell getprop ro.bootloader");
    }

    public static Route kernelSu() {
        return new Route("kernelsu", "KernelSU compatibility review", "ADVANCED ROUTE",
                "A kernel-level option only when the project explicitly supports your exact device and kernel.",
                "Android version or a GKI label alone does not establish compatibility. No kernel is bundled or flashed by this app.",
                list("A device and kernel combination supported by the official KernelSU documentation.",
                        "An officially unlocked bootloader and the exact matching stock images.",
                        "Experience recovering a device from a failed boot.",
                        "A deliberate choice between root implementations; do not stack them blindly."),
                list("Read KernelSU's official installation and device-support documentation.",
                        "Verify the kernel release, device support status, and recommended installation method.",
                        "If—and only if—your combination is supported, follow that project's exact instructions.",
                        "Use the official manager to review app grants after installation.",
                        "If compatibility is unclear, stop and ask the device maintainer rather than flashing a generic kernel."),
                "An incompatible kernel can prevent boot. This app does not test kernels by flashing them and does not claim KernelSU works on every GKI device.",
                KERNELSU_INSTALL, "Open KernelSU installation guide", "adb shell uname -r");
    }

    public static Route recovery() {
        return new Route("recovery", "Legacy recovery documentation", "DEPRECATED ROUTE",
                "Some older devices have a maintained, model-specific recovery installation path. It is not a universal rooting method.",
                "Consider only for a documented older device. Modern devices should use their supported image-patching workflow.",
                list("A trusted recovery specifically maintained for the exact device codename.",
                        "Official support for the intended installation method.",
                        "An officially unlockable bootloader where required.",
                        "Matching stock firmware and a recovery plan."),
                list("Identify the exact device codename, not just the retail model name.",
                        "Read the current Magisk custom-recovery section and your recovery maintainer's instructions.",
                        "Confirm that the legacy route is still supported for your firmware.",
                        "If supported, follow those model-specific instructions outside TrillRoot.",
                        "If the method is deprecated or unclear, stop rather than flashing an old package."),
                "Magisk deprecates custom-recovery installation. Old recoveries and one-click rooting packages may be unmaintained or unsafe. This route is guidance, not an automated fallback.",
                MAGISK_INSTALL + "#custom-recovery", "Read recovery limitations", "adb shell getprop ro.product.device");
    }

    public static List<Route> ordered(DeviceInfo info, RootAccess access) {
        List<Route> result = new ArrayList<>();
        if (access != null && access.isConfirmed()) result.add(existing());
        if (info != null && info.isSamsung()) {
            result.add(samsung());
            result.add(magisk());
        } else {
            result.add(magisk());
        }
        result.add(kernelSu());
        // Keep legacy limitations discoverable on every device, without suggesting it is recommended.
        result.add(recovery());
        return result;
    }

    public static String recommendation(DeviceInfo info, RootAccess access) {
        if (access != null && access.isConfirmed()) return "Review your existing root manager";
        if (info == null) return "Read your device information first";
        if (info.isSamsung()) return "Start with the Samsung-specific guide";
        if (PropertySignals.bootloaderLock(info) == PropertySignals.Lock.LOCKED) {
            return "Check official unlock eligibility first";
        }
        return "Evaluate the Magisk image-patching route";
    }

    private static List<String> list(String... values) { return Arrays.asList(values); }
}
