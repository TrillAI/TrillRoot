package com.trillai.trillroot.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Conservative interpretation of optional, OEM-controlled system properties. */
public final class PropertySignals {
    public static final String FLASH_LOCKED = "ro.boot.flash.locked";
    public static final String VBMETA_STATE = "ro.boot.vbmeta.device_state";
    public static final String VERIFIED_BOOT = "ro.boot.verifiedbootstate";
    public static final String OEM_SUPPORTED = "ro.oem_unlock_supported";
    public static final String OEM_ALLOWED = "sys.oem_unlock_allowed";
    public static final String SLOT = "ro.boot.slot_suffix";
    public static final String DYNAMIC = "ro.boot.dynamic_partitions";
    public static final String FIRST_API = "ro.product.first_api_level";
    public static final String BUILD_TYPE = "ro.build.type";
    public static final String DEBUGGABLE = "ro.debuggable";

    public static final Set<String> ALLOWED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            FLASH_LOCKED, VBMETA_STATE, VERIFIED_BOOT, OEM_SUPPORTED, OEM_ALLOWED,
            SLOT, DYNAMIC, FIRST_API, BUILD_TYPE, DEBUGGABLE)));

    public enum Signal { YES, NO, UNKNOWN }
    public enum Lock { LOCKED, UNLOCKED, UNKNOWN }

    private PropertySignals() { }

    private static Signal yesNo(String value) {
        if ("1".equals(value)) return Signal.YES;
        if ("0".equals(value)) return Signal.NO;
        return Signal.UNKNOWN;
    }

    /** Conflicting or absent signals are unknown. Verified-boot color alone is not a lock state. */
    public static Lock bootloaderLock(String flashLocked, String vbmetaState) {
        Signal flash = yesNo(flashLocked);
        String vbmeta = normalize(vbmetaState);
        Signal vendor = "locked".equals(vbmeta) ? Signal.YES
                : "unlocked".equals(vbmeta) ? Signal.NO : Signal.UNKNOWN;
        if (flash == Signal.YES && vendor != Signal.NO) return Lock.LOCKED;
        if (flash == Signal.NO && vendor != Signal.YES) return Lock.UNLOCKED;
        if (flash == Signal.UNKNOWN) {
            if (vendor == Signal.YES) return Lock.LOCKED;
            if (vendor == Signal.NO) return Lock.UNLOCKED;
        }
        return Lock.UNKNOWN;
    }

    public static Lock bootloaderLock(DeviceInfo info) {
        return bootloaderLock(info.property(FLASH_LOCKED), info.property(VBMETA_STATE));
    }

    /** This is capability reported by the vendor, not permission to unlock a particular unit. */
    public static Signal unlockSupport(String value) { return yesNo(value); }

    public static String lockLabel(DeviceInfo info) {
        switch (bootloaderLock(info)) {
            case LOCKED: return "Locked (reported)";
            case UNLOCKED: return "Unlocked (reported)";
            default: return "Unconfirmed";
        }
    }

    public static String supportLabel(DeviceInfo info) {
        switch (unlockSupport(info.property(OEM_SUPPORTED))) {
            case YES: return "Reported supported; verify eligibility";
            case NO: return "Reported unsupported";
            default: return "Not exposed";
        }
    }

    public static String verifiedBootLabel(DeviceInfo info) {
        String value = normalize(info.property(VERIFIED_BOOT));
        switch (value) {
            case "green": return "Green";
            case "yellow": return "Yellow";
            case "orange": return "Orange";
            case "red": return "Red";
            default: return "Not exposed";
        }
    }

    public static String slotLabel(DeviceInfo info) {
        String slot = normalize(info.property(SLOT));
        return "_a".equals(slot) ? "A (reported)" : "_b".equals(slot) ? "B (reported)"
                : "Not reported / may be A-only";
    }

    public static String dynamicLabel(DeviceInfo info) {
        Signal signal = yesNo(normalize(info.property(DYNAMIC)));
        if (signal == Signal.UNKNOWN && "true".equals(normalize(info.property(DYNAMIC)))) signal = Signal.YES;
        if (signal == Signal.UNKNOWN && "false".equals(normalize(info.property(DYNAMIC)))) signal = Signal.NO;
        return signal == Signal.YES ? "Yes (reported)" : signal == Signal.NO ? "No (reported)" : "Not exposed";
    }

    public static int firstApi(DeviceInfo info) {
        try {
            int value = Integer.parseInt(info.property(FIRST_API));
            return value >= 1 && value <= info.sdk ? value : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static boolean isEmulatorHint(DeviceInfo info) {
        String f = info.fingerprint.toLowerCase(Locale.ROOT);
        return f.contains("generic") || f.contains("emulator") || f.contains("sdk_gphone");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
