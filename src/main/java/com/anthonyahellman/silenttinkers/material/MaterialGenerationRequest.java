package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable handoff between planning and ecosystem-specific generation.
 * Nothing is registered here; this simply states what SilentTinkers intends to
 * create and why, making the same information usable by diagnostics and UI.
 */
public record MaterialGenerationRequest(
        ResourceLocation physicalItem,
        MaterialBridgePlan.Action action,
        Optional<MaterialProfile.Ecosystem> source,
        Optional<MaterialProfile.Ecosystem> target,
        Optional<BootstrapMaterialProfile> bootstrapProfile) {

    public MaterialGenerationRequest {
        Objects.requireNonNull(physicalItem, "physicalItem");
        Objects.requireNonNull(action, "action");
        source = Objects.requireNonNull(source, "source");
        target = Objects.requireNonNull(target, "target");
        bootstrapProfile = Objects.requireNonNull(bootstrapProfile, "bootstrapProfile");
    }

    public boolean generatesAnything() {
        return action != MaterialBridgePlan.Action.PRESERVE;
    }
}
