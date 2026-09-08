package com.anthonyahellman.silenttinkers.material;

import java.util.concurrent.atomic.AtomicReference;

/** Runtime evidence that the native Silent Gear charger actually rewrote a composite material. */
public final class StarChargeBridgeHealth {
    private static final AtomicReference<Status> STATUS = new AtomicReference<>(Status.NOT_OBSERVED);

    private StarChargeBridgeHealth() {}

    public static Status status() {
        return STATUS.get();
    }

    public static void markApplied() {
        STATUS.set(Status.APPLIED);
    }

    public static void markFailed() {
        STATUS.set(Status.FAILED);
    }

    public static void clear() {
        STATUS.set(Status.NOT_OBSERVED);
    }

    public enum Status {
        NOT_OBSERVED,
        APPLIED,
        FAILED
    }
}
