package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/** Immutable handoff between planning and ecosystem-specific generation. */
public record MaterialGenerationRequest(
        ResourceLocation physicalItem,
        MaterialBridgePlan.Action action,
        Optional<MaterialProfile.Ecosystem> source,
        Optional<MaterialProfile.Ecosystem> target,
        Optional<ResourceLocation> sourceMaterialId,
        Optional<ResourceLocation> targetMaterialId,
        Optional<BootstrapMaterialProfile> bootstrapProfile) {

    public MaterialGenerationRequest {
        Objects.requireNonNull(physicalItem, "physicalItem");
        Objects.requireNonNull(action, "action");
        source = Objects.requireNonNull(source, "source");
        target = Objects.requireNonNull(target, "target");
        sourceMaterialId = Objects.requireNonNull(sourceMaterialId, "sourceMaterialId");
        targetMaterialId = Objects.requireNonNull(targetMaterialId, "targetMaterialId");
        bootstrapProfile = Objects.requireNonNull(bootstrapProfile, "bootstrapProfile");
        if (action == MaterialBridgePlan.Action.BRIDGE
                && (source.isEmpty() || target.isEmpty()
                || sourceMaterialId.isEmpty() || targetMaterialId.isEmpty())) {
            throw new IllegalArgumentException("Bridge generation requires canonical source and target material ids");
        }
        if (target.isPresent() != targetMaterialId.isPresent()) {
            throw new IllegalArgumentException("target ecosystem and target material id must be present together");
        }
    }

    public boolean generatesAnything() {
        return action != MaterialBridgePlan.Action.PRESERVE;
    }
}
