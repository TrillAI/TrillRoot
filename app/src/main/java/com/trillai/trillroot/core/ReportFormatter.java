package com.trillai.trillroot.core;

import java.util.Set;

/** User-initiated export. No unique hardware identifiers or raw command output. */
public final class ReportFormatter {
    private ReportFormatter() { }

    public static String format(DeviceInfo d, RootAccess root, Set<String> checked) {
        StringBuilder out = new StringBuilder(2048);
        out.append("TRILLROOT READINESS REPORT\n")
                .append("Informational only — not a compatibility certificate.\n\n")
                .append("DEVICE\n").append("Manufacturer: ").append(d.manufacturer)
                .append("\nBrand: ").append(d.brand).append("\nModel: ").append(d.model)
                .append("\nCodename: ").append(d.device).append("\nAndroid: ").append(d.androidVersion)
                .append(" (API ").append(d.sdk).append(")\nSecurity patch: ").append(d.securityPatch)
                .append("\nABI: ").append(d.abi).append("\nBuild: ").append(d.displayId)
                .append("\nIncremental: ").append(d.incremental).append("\nFingerprint: ").append(d.fingerprint)
                .append("\n\nBOOT SIGNALS (OEM-CONTROLLED, NOT ATTESTED)\n")
                .append("Bootloader: ").append(PropertySignals.lockLabel(d))
                .append("\nOEM unlock capability: ").append(PropertySignals.supportLabel(d))
                .append("\nVerified-boot state: ").append(PropertySignals.verifiedBootLabel(d))
                .append("\nSlot: ").append(PropertySignals.slotLabel(d))
                .append("\nDynamic partitions: ").append(PropertySignals.dynamicLabel(d));
        int firstApi = PropertySignals.firstApi(d);
        out.append("\nLaunch API: ").append(firstApi > 0 ? Integer.toString(firstApi) : "Not exposed");
        out.append("\n\nROOT CHECK\n").append(root.label()).append('\n').append(root.detail)
                .append("\n\nNEXT ROUTE TO EVALUATE\n").append(RouteEngine.recommendation(d, root))
                .append("\n\nPREPARATION CHECKLIST\n");
        for (Checklist.Item item : Checklist.ITEMS) {
            out.append(checked.contains(item.id) ? "[x] " : "[ ] ").append(item.title).append('\n');
        }
        out.append("\nBOUNDARIES\nNo exploit, unlock, flash, or root installation was performed by TrillRoot. ")
                .append("A supported route depends on the exact device and firmware. Never relock with modified images. ")
                .append("This report contains build information; review before sharing.\n");
        return out.toString();
    }
}
