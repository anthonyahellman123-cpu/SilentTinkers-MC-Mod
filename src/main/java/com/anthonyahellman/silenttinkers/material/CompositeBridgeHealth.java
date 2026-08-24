package com.anthonyahellman.silenttinkers.material;

import java.util.concurrent.atomic.AtomicReference;

/** Latest validated health of the static Tinkers trait hook used by dynamic composite tools. */
public final class CompositeBridgeHealth {
    private static final AtomicReference<Status> STATUS = new AtomicReference<>(Status.UNKNOWN);

    private CompositeBridgeHealth() {}

    public static Status status() {
        return STATUS.get();
    }

    public static void set(Status status) {
        STATUS.set(status);
    }

    public static void clear() {
        STATUS.set(Status.UNKNOWN);
    }

    public enum Status {
        UNKNOWN,
        BOUND,
        TRAIT_MISSING,
        HOOK_MISMATCH
    }
}
