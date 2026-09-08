package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only decision produced before SilentTinkers mutates or generates any
 * compatibility data. Material IDs are carried here so downstream generation
 * never has to reverse-resolve identity from a representative physical alias.
 */
public record MaterialBridgePlan(
        ResourceLocation physicalItem,
        Action action,
        Optional<MaterialProfile.Ecosystem> source,
        Optional<MaterialProfile.Ecosystem> target,
        Optional<ResourceLocation> sourceMaterialId,
        Optional<ResourceLocation> targetMaterialId,
        Reason reason) {

    public MaterialBridgePlan {
        Objects.requireNonNull(physicalItem, "physicalItem");
        Objects.requireNonNull(action, "action");
        source = Objects.requireNonNull(source, "source");
        target = Objects.requireNonNull(target, "target");
        sourceMaterialId = Objects.requireNonNull(sourceMaterialId, "sourceMaterialId");
        targetMaterialId = Objects.requireNonNull(targetMaterialId, "targetMaterialId");
        Objects.requireNonNull(reason, "reason");
        if (source.isPresent() != sourceMaterialId.isPresent()) {
            throw new IllegalArgumentException("source ecosystem and source material id must be present together");
        }
        if (target.isPresent() != targetMaterialId.isPresent()) {
            throw new IllegalArgumentException("target ecosystem and target material id must be present together");
        }
        if (action == Action.BRIDGE && (source.isEmpty() || target.isEmpty())) {
            throw new IllegalArgumentException("Bridge plans require owned source and target identities");
        }
    }

    public enum Action {
        PRESERVE,
        BRIDGE,
        BOOTSTRAP
    }

    public enum Reason {
        BOTH_PRESENT,
        SILENT_GEAR_ONLY,
        TINKERS_ONLY,
        EXTERNAL_MATERIAL
    }
}
