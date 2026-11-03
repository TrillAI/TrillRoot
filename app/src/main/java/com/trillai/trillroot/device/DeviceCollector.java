package com.trillai.trillroot.device;

import android.os.Build;

import com.trillai.trillroot.core.DeviceInfo;
import com.trillai.trillroot.core.PropertySignals;
import com.trillai.trillroot.core.RootAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Only fixed, non-mutating commands. Root is requested only by probeRoot(). */
public final class DeviceCollector {
    public static final List<String> ROOT_COMMAND = Collections.unmodifiableList(Arrays.asList("su", "-c", "id"));
    private static final List<String> PROPERTY_ORDER = Collections.unmodifiableList(Arrays.asList(
            PropertySignals.FLASH_LOCKED, PropertySignals.VBMETA_STATE,
            PropertySignals.VERIFIED_BOOT, PropertySignals.OEM_SUPPORTED,
            PropertySignals.OEM_ALLOWED, PropertySignals.SLOT,
            PropertySignals.DYNAMIC, PropertySignals.FIRST_API,
            PropertySignals.BUILD_TYPE, PropertySignals.DEBUGGABLE));
    private final CommandRunner runner;

    public DeviceCollector(CommandRunner runner) { this.runner = runner; }

    public DeviceInfo collect() {
        DeviceInfo.Builder builder = new DeviceInfo.Builder()
                .manufacturer(Build.MANUFACTURER).brand(Build.BRAND).model(Build.MODEL)
                .device(Build.DEVICE).androidVersion(Build.VERSION.RELEASE).sdk(Build.VERSION.SDK_INT)
                .fingerprint(Build.FINGERPRINT).incremental(Build.VERSION.INCREMENTAL)
                .displayId(Build.DISPLAY);
        if (Build.VERSION.SDK_INT >= 23) builder.securityPatch(Build.VERSION.SECURITY_PATCH);
        if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null) {
            StringBuilder abis = new StringBuilder();
            for (String abi : Build.SUPPORTED_ABIS) {
                if (abis.length() > 0) abis.append(", ");
                abis.append(abi);
            }
            builder.abi(abis.toString());
        }
        for (String key : PROPERTY_ORDER) {
            if (Thread.currentThread().isInterrupted()) break;
            CommandRunner.Result result = runner.run(Arrays.asList("/system/bin/getprop", key), 1500, 512);
            if (result.completed && result.exitCode == 0) {
                builder.property(key, result.output.trim());
            }
        }
        return builder.build();
    }

    public RootAccess probeRoot() {
        // The only elevated command in the app. It is read-only and explicitly consented to in the UI.
        CommandRunner.Result result = runner.run(ROOT_COMMAND, 20000, 8192);
        if (Thread.currentThread().isInterrupted()) return RootAccess.initial();
        if (result.timedOut) return new RootAccess(RootAccess.State.TIMEOUT,
                "No grant was received within 20 seconds. The request was stopped; no retry or alternative elevation attempt was made.");
        if (!result.completed) return new RootAccess(RootAccess.State.ERROR,
                "The su command could not be completed. It may be absent or blocked by Android policy. This does not prove the device is unrooted.");
        return RootAccess.fromCommand(result.exitCode, result.output);
    }

    public static List<String> propertyKeys() { return new ArrayList<>(PROPERTY_ORDER); }
}
