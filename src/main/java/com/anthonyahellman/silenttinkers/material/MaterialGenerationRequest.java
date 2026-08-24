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
        Optional<BootstrapMaterialProfile> bootstrapProfile) {

    public MaterialGenerationRequest {
        Objects.requireNonNull(physicalItem, "physicalItem");
        Objects.requireNonNull(action, "action");
        source = Objects.requireNonNull(source, "source");
        target = Objects.requireNonNull(target, "target");
        sourceMaterialId = Objects.requireNonNull(sourceMaterialId, "sourceMaterialId");
        bootstrapProfile = Objects.requireNonNull(bootstrapProfile, "bootstrapProfile");
        if (action == MaterialBridgePlan.Action.BRIDGE && sourceMaterialId.isEmpty()) {
            throw new IllegalArgumentException("Bridge generation requires a canonical source material id");
        }
    }

    public boolean generatesAnything() {
        return action != MaterialBridgePlan.Action.PRESERVE;
    }
}
