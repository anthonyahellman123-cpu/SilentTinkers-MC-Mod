package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authoritative lookup for pack-authored material tuning.
 *
 * <p>This starts as an in-memory boundary on purpose. A later datapack/config
 * loader can replace the contents without changing bootstrap generation or the
 * planner UI.</p>
 */
public final class MaterialOverrideRegistry {
    private static volatile Map<ResourceLocation, MaterialStatOverride> overrides = Map.of();

    private MaterialOverrideRegistry() {}

    public static Optional<MaterialStatOverride> find(ResourceLocation physicalItem) {
        return Optional.ofNullable(overrides.get(physicalItem));
    }

    public static Collection<MaterialStatOverride> all() {
        return overrides.values();
    }

    public static synchronized void replace(Collection<MaterialStatOverride> authored) {
        Map<ResourceLocation, MaterialStatOverride> next = new LinkedHashMap<>();
        for (MaterialStatOverride override : authored) {
            next.put(override.physicalItem(), override);
        }
        overrides = Map.copyOf(next);
    }

    public static synchronized void clear() {
        overrides = Map.of();
    }
}
