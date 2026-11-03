package com.trillai.trillroot.core;

import com.trillai.trillroot.device.CommandRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Dependency-free JVM regression tests; run by tools/build_apk.py. */
public final class CoreTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        rootClassification();
        propertyInterpretation();
        routeSelection();
        rootabilityAssessment();
        checklistAndReport();
        commandRunner();
        System.out.println("PASS: " + checks + " regression checks");
    }

    private static void rootClassification() {
        equal(RootAccess.State.GRANTED, RootAccess.classify(0, "uid=0(root) gid=0(root) groups=0(root)"));
        equal(RootAccess.State.GRANTED, RootAccess.classify(0, "\nuid=0(root) gid=0(root)\r\n"));
        equal(RootAccess.State.NOT_GRANTED, RootAccess.classify(1, "uid=0(root)"));
        equal(RootAccess.State.NOT_GRANTED, RootAccess.classify(0, "uid=2000(shell) gid=2000(shell)"));
        equal(RootAccess.State.NOT_GRANTED, RootAccess.classify(0, "fake uid=0(root)"));
        equal(RootAccess.State.NOT_GRANTED, RootAccess.classify(0, "euid=0(root)"));
        equal(RootAccess.State.NOT_GRANTED, RootAccess.classify(0, "uid=0(root)extra"));
        equal(RootAccess.State.NOT_GRANTED, RootAccess.classify(0, ""));
        equal(RootAccess.State.NOT_GRANTED, RootAccess.classify(0, null));
        check(!RootAccess.initial().isConfirmed(), "initial state must not claim root");
        check(RootAccess.fromCommand(0, "uid=0(root)").isConfirmed(), "confirmed result");
        check(!new RootAccess(RootAccess.State.TIMEOUT, "").isConfirmed(), "timeout is not root");
    }

    private static void propertyInterpretation() {
        equal(PropertySignals.Lock.LOCKED, PropertySignals.bootloaderLock("1", "locked"));
        equal(PropertySignals.Lock.UNLOCKED, PropertySignals.bootloaderLock("0", "unlocked"));
        equal(PropertySignals.Lock.LOCKED, PropertySignals.bootloaderLock("1", ""));
        equal(PropertySignals.Lock.UNLOCKED, PropertySignals.bootloaderLock("0", ""));
        equal(PropertySignals.Lock.UNKNOWN, PropertySignals.bootloaderLock("0", "locked"));
        equal(PropertySignals.Lock.UNKNOWN, PropertySignals.bootloaderLock("1", "unlocked"));
        equal(PropertySignals.Lock.UNKNOWN, PropertySignals.bootloaderLock("", ""));
        equal(PropertySignals.Lock.UNKNOWN, PropertySignals.bootloaderLock("", "orange"));
        equal(PropertySignals.Signal.UNKNOWN, PropertySignals.unlockSupport(""));
        equal(PropertySignals.Signal.NO, PropertySignals.unlockSupport("0"));
        equal(PropertySignals.Signal.YES, PropertySignals.unlockSupport("1"));
        DeviceInfo missing = new DeviceInfo.Builder().sdk(36).build();
        equal("Unconfirmed", PropertySignals.lockLabel(missing));
        equal("Not exposed", PropertySignals.supportLabel(missing));
        equal(-1, PropertySignals.firstApi(missing));
        DeviceInfo invalid = new DeviceInfo.Builder().sdk(35)
                .property(PropertySignals.FIRST_API, "36").build();
        equal(-1, PropertySignals.firstApi(invalid));
        DeviceInfo pixel = new DeviceInfo.Builder().sdk(36).manufacturer("Google")
                .property(PropertySignals.FIRST_API, "34")
                .property(PropertySignals.SLOT, "_b")
                .property(PropertySignals.DYNAMIC, "true")
                .property(PropertySignals.VERIFIED_BOOT, "orange")
                .property("ro.serialno", "must-not-be-retained").build();
        equal(34, PropertySignals.firstApi(pixel));
        equal("B (reported)", PropertySignals.slotLabel(pixel));
        equal("Yes (reported)", PropertySignals.dynamicLabel(pixel));
        equal("Orange", PropertySignals.verifiedBootLabel(pixel));
        equal("", pixel.property("ro.serialno"));
        check(PropertySignals.isEmulatorHint(new DeviceInfo.Builder().fingerprint("google/sdk_gphone64_x86_64/test").build()), "emulator hint");
    }

    private static void routeSelection() {
        DeviceInfo samsung = new DeviceInfo.Builder().manufacturer("samsung").model("SM-test").sdk(36).build();
        DeviceInfo pixel = new DeviceInfo.Builder().manufacturer("Google").model("Pixel test").sdk(36).build();
        DeviceInfo legacy = new DeviceInfo.Builder().manufacturer("Test").sdk(23).build();
        check(samsung.isSamsung(), "case-insensitive vendor detection");
        equal("samsung", RouteEngine.ordered(samsung, RootAccess.initial()).get(0).id);
        equal("magisk", RouteEngine.ordered(pixel, RootAccess.initial()).get(0).id);
        equal("existing", RouteEngine.ordered(pixel, RootAccess.fromCommand(0, "uid=0(root)")).get(0).id);
        equal("Review your existing root manager", RouteEngine.recommendation(samsung, RootAccess.fromCommand(0, "uid=0(root)")));
        for (DeviceInfo device : Arrays.asList(samsung, pixel, legacy)) {
            List<Route> routes = RouteEngine.ordered(device, RootAccess.initial());
            HashSet<String> ids = new HashSet<>();
            for (Route route : routes) {
                check(ids.add(route.id), "no duplicate routes");
                check(route.documentation.startsWith("https://"), "HTTPS official documentation");
                check(route.readOnlyCommand.startsWith("adb "), "only copyable read-only ADB commands");
                check(!route.readOnlyCommand.contains("flash"), "no flashing command");
                check(!route.requirements.isEmpty() && !route.steps.isEmpty(), "complete guidance");
                check(!route.warning.isEmpty(), "explicit risk warning");
            }
            check(ids.contains("recovery"), "legacy limitations remain discoverable");
            check(ids.contains("kernelsu"), "kernel compatibility is not assumed");
        }
        DeviceInfo locked = new DeviceInfo.Builder().sdk(36)
                .property(PropertySignals.FLASH_LOCKED, "1").build();
        equal("Check official unlock eligibility first", RouteEngine.recommendation(locked, RootAccess.initial()));
        check(RouteEngine.magisk().steps.get(2).contains("init_boot"), "modern partition alternatives explained");
        check(RouteEngine.samsung().warning.contains("FRP"), "no protection bypass");
    }

    private static void rootabilityAssessment() {
        equal(RootabilityAssessment.Verdict.UNKNOWN,
                RootabilityAssessment.evaluate(null, RootAccess.initial()).verdict);
        DeviceInfo unsupported = new DeviceInfo.Builder().sdk(36)
                .property(PropertySignals.OEM_SUPPORTED, "0").build();
        equal(RootabilityAssessment.Verdict.NOT_OFFICIALLY_SUPPORTED,
                RootabilityAssessment.evaluate(unsupported, RootAccess.initial()).verdict);
        DeviceInfo eligible = new DeviceInfo.Builder().sdk(36)
                .property(PropertySignals.OEM_SUPPORTED, "1")
                .property(PropertySignals.FLASH_LOCKED, "0").build();
        equal(RootabilityAssessment.Verdict.ROUTE_POSSIBLE,
                RootabilityAssessment.evaluate(eligible, RootAccess.initial()).verdict);
        equal(RootabilityAssessment.Verdict.ALREADY_ROOTED,
                RootabilityAssessment.evaluate(unsupported,
                        RootAccess.fromCommand(0, "uid=0(root)")).verdict);
        check(RootabilityAssessment.evaluate(new DeviceInfo.Builder().sdk(36).build(),
                RootAccess.initial()).explanation.contains("Unknown"),
                "unknown never triggers an exploit fallback");
    }

    private static void checklistAndReport() {
        equal(0, Checklist.completed(Collections.emptySet()));
        equal(2, Checklist.completed(Arrays.asList("backup", "ownership", "backup", "unknown")));
        equal(8, Checklist.ITEMS.size());
        DeviceInfo d = new DeviceInfo.Builder().manufacturer("Google").model("Test phone")
                .device("test").sdk(36).androidVersion("16").fingerprint("google/test/test:16/build")
                .property(PropertySignals.FLASH_LOCKED, "0").build();
        String report = ReportFormatter.format(d, RootAccess.initial(), new HashSet<>(Collections.singletonList("backup")));
        check(report.contains("not a compatibility certificate"), "report disclaimer");
        check(report.contains("Root access not checked"), "honest initial root result");
        check(report.contains("[x] My data is backed up and verified"), "checked export");
        check(report.contains("[ ] I own this device"), "unchecked export");
        check(!report.contains("must-not-be-retained"), "no private properties in report");
        check(report.contains("No exploit, unlock, flash"), "explicit non-mutation statement");
    }

    private static void commandRunner() throws Exception {
        try (CommandRunner runner = new CommandRunner()) {
            CommandRunner.Result normal = runner.run(Arrays.asList("/bin/sh", "-c", "printf 'uid=0(root)\\n'"), 2000, 128);
            check(normal.completed && normal.exitCode == 0, "fixed command executes");
            equal(RootAccess.State.GRANTED, RootAccess.classify(normal.exitCode, normal.output));
            CommandRunner.Result failure = runner.run(Arrays.asList("/bin/sh", "-c", "exit 7"), 2000, 128);
            check(failure.completed && failure.exitCode == 7, "nonzero status retained");
            CommandRunner.Result capped = runner.run(Arrays.asList("/bin/sh", "-c", "printf '%020000d' 0"), 3000, 128);
            check(capped.completed && capped.output.length() == 128, "output is bounded");
            long start = System.nanoTime();
            CommandRunner.Result timeout = runner.run(Arrays.asList("/bin/sh", "-c", "sleep 10"), 200, 128);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            check(!timeout.completed && timeout.timedOut, "deadline result is explicit");
            check(elapsed < 3000, "deadline does not wait for command completion");
            CommandRunner.Result literal = runner.run(Arrays.asList("/bin/echo", "hello; touch should-not-exist"), 2000, 128);
            check(literal.output.trim().equals("hello; touch should-not-exist"), "arguments are not shell-interpolated");
        }
        CommandRunner closed = new CommandRunner();
        closed.close();
        check(!closed.run(Arrays.asList("/bin/echo", "no"), 1000, 128).completed, "closed runner rejects work");
    }

    private static void equal(Object expected, Object actual) {
        check(expected.equals(actual), "expected <" + expected + "> but got <" + actual + ">");
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
