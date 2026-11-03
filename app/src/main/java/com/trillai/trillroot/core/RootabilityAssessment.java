package com.trillai.trillroot.core;

/**
 * Conservative, explainable readiness result derived only from public build signals.
 * This is not a vulnerability scan and never treats an unknown value as permission to exploit.
 */
public final class RootabilityAssessment {
    public enum Verdict { ALREADY_ROOTED, ROUTE_POSSIBLE, NOT_OFFICIALLY_SUPPORTED, UNKNOWN }

    public final Verdict verdict;
    public final String title;
    public final String explanation;

    private RootabilityAssessment(Verdict verdict, String title, String explanation) {
        this.verdict = verdict;
        this.title = title;
        this.explanation = explanation;
    }

    public static RootabilityAssessment evaluate(DeviceInfo info, RootAccess access) {
        if (access != null && access.isConfirmed()) {
            return new RootabilityAssessment(Verdict.ALREADY_ROOTED, "Root access confirmed",
                    "An existing su manager granted UID 0. No additional rooting method is needed.");
        }
        if (info == null) {
            return new RootabilityAssessment(Verdict.UNKNOWN, "Not enough information",
                    "Device information has not been read yet.");
        }
        PropertySignals.Signal support = PropertySignals.unlockSupport(
                info.property(PropertySignals.OEM_SUPPORTED));
        PropertySignals.Lock lock = PropertySignals.bootloaderLock(info);
        if (support == PropertySignals.Signal.NO) {
            return new RootabilityAssessment(Verdict.NOT_OFFICIALLY_SUPPORTED,
                    "No supported route reported",
                    "This build reports that OEM unlocking is unsupported. Confirm with the manufacturer and carrier; Trill Root will not bypass locks or account policy.");
        }
        if (support == PropertySignals.Signal.YES && lock == PropertySignals.Lock.UNLOCKED) {
            return new RootabilityAssessment(Verdict.ROUTE_POSSIBLE,
                    "A documented route may be possible",
                    "OEM unlocking is reported supported and the bootloader is reported unlocked. Exact firmware and model support still must be verified.");
        }
        if (support == PropertySignals.Signal.YES) {
            return new RootabilityAssessment(Verdict.ROUTE_POSSIBLE,
                    "Check official unlock eligibility",
                    "OEM unlocking is reported supported, but eligibility and bootloader state require confirmation in the manufacturer's interface.");
        }
        if (lock == PropertySignals.Lock.UNLOCKED) {
            return new RootabilityAssessment(Verdict.ROUTE_POSSIBLE,
                    "A documented route may be possible",
                    "The bootloader is reported unlocked. Verify exact firmware and a maintained root project before changing images.");
        }
        return new RootabilityAssessment(Verdict.UNKNOWN, "Rootability is unknown",
                "Android does not expose enough trustworthy information to decide. Unknown is not permission to run legacy exploits.");
    }

    private RootabilityAssessment() { this(null, "", ""); }
}
