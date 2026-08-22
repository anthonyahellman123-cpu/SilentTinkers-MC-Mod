package com.anthonyahellman.silenttinkers.material;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the latest completed read-only material discovery snapshot.
 *
 * <p>This gives later bridge generation, commands, and the planner UI one
 * authoritative view of the currently loaded datapack state instead of making
 * each consumer rescan Silent Gear and Tinkers independently.</p>
 */
public final class MaterialDiscoveryState {
    private static final AtomicReference<UnifiedMaterialDiscovery.Snapshot> CURRENT =
            new AtomicReference<>();

    private MaterialDiscoveryState() {}

    public static void publish(UnifiedMaterialDiscovery.Snapshot snapshot) {
        CURRENT.set(snapshot);
    }

    public static Optional<UnifiedMaterialDiscovery.Snapshot> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.set(null);
    }
}
