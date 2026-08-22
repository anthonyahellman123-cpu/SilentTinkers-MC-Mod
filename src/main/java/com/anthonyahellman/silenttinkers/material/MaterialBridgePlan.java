package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only decision produced before SilentTinkers mutates or generates any
 * compatibility data. Keeping planning separate makes generation auditable and
 * gives the future planner UI the same answer as the automatic bridge engine.
 */
public record MaterialBridgePlan(
        ResourceLocation physicalItem,
        Action action,
        Optional<MaterialProfile.Ecosystem> source,
        Optional<MaterialProfile.Ecosystem> target,
        Reason reason) {

    public MaterialBridgePlan {
        Objects.requireNonNull(physicalItem, "physicalItem");
        Objects.requireNonNull(action, "action");
        source = Objects.requireNonNull(source, "source");
        target = Objects.requireNonNull(target, "target");
        Objects.requireNonNull(reason, "reason");
    }

    public enum Action {
        /** Both equipment ecosystems already understand this physical material. */
        PRESERVE,
        /** Exactly one ecosystem understands it; generate only the missing side. */
        BRIDGE,
        /** Neither ecosystem understands it; external discovery must seed both. */
        BOOTSTRAP
    }

    public enum Reason {
        BOTH_PRESENT,
        SILENT_GEAR_ONLY,
        TINKERS_ONLY,
        EXTERNAL_MATERIAL
    }
}
