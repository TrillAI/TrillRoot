package com.trillai.trillroot.core;

import java.util.regex.Pattern;

/** A result from the single, user-authorized `su -c id` probe. Never an acquisition attempt. */
public final class RootAccess {
    public enum State { NOT_CHECKED, CHECKING, GRANTED, NOT_GRANTED, TIMEOUT, ERROR }
    private static final Pattern UID_ZERO = Pattern.compile("^uid=0\\([^)]*\\)(?:\\s|$)");

    public final State state;
    public final String detail;

    public RootAccess(State state, String detail) {
        this.state = state;
        this.detail = detail;
    }

    public static RootAccess initial() {
        return new RootAccess(State.NOT_CHECKED, "No root request has been made. The check is always optional.");
    }

    public static State classify(int exitCode, String output) {
        if (exitCode != 0 || output == null) return State.NOT_GRANTED;
        String normalized = output.replace("\r\n", "\n").trim();
        return UID_ZERO.matcher(normalized).find() ? State.GRANTED : State.NOT_GRANTED;
    }

    public static RootAccess fromCommand(int exitCode, String output) {
        State result = classify(exitCode, output);
        return new RootAccess(result, result == State.GRANTED
                ? "su returned UID 0 for the id command. This confirms a root grant, not the safety or provenance of your root manager."
                : "No UID 0 grant was returned. Access may be absent, denied, restricted, or managed differently; this is not proof that the device is unrooted.");
    }

    public String label() {
        switch (state) {
            case GRANTED: return "Root access granted";
            case NOT_GRANTED: return "No root grant detected";
            case CHECKING: return "Waiting for your root manager…";
            case TIMEOUT: return "Root check timed out";
            case ERROR: return "Root check unavailable";
            default: return "Root access not checked";
        }
    }

    public boolean isConfirmed() { return state == State.GRANTED; }
}
