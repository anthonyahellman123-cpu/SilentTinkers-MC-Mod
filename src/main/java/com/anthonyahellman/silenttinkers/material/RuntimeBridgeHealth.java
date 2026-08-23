package com.anthonyahellman.silenttinkers.material;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tiny process-local health latch for the dynamic SG -> Tinkers bridge.
 * Discovery readiness and actual tool-build execution are intentionally separate:
 * a scan can be healthy before any player has assembled a composite tool.
 */
public final class RuntimeBridgeHealth {
    private static final AtomicBoolean COMPOSITE_STATS_APPLIED = new AtomicBoolean(false);

    private RuntimeBridgeHealth() {}

    public static void markCompositeStatsApplied() {
        COMPOSITE_STATS_APPLIED.set(true);
    }

    public static boolean compositeStatsApplied() {
        return COMPOSITE_STATS_APPLIED.get();
    }

    public static void clear() {
        COMPOSITE_STATS_APPLIED.set(false);
    }
}
