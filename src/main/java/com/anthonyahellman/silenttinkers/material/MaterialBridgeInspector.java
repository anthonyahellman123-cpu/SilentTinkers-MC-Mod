package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Small, dependency-free diagnostic surface for the unified material model.
 * This intentionally knows nothing about smelting/casting; it only reports
 * what the registry believes exists in each equipment ecosystem.
 */
public final class MaterialBridgeInspector {
    private MaterialBridgeInspector() {}

    public static Inspection inspect(ResourceLocation canonicalId) {
        return UnifiedMaterialRegistry.get(canonicalId)
                .map(MaterialBridgeInspector::inspect)
                .orElseGet(() -> Inspection.missing(canonicalId));
    }

    public static Inspection inspect(UnifiedMaterial material) {
        List<String> profiles = new ArrayList<>();
        for (MaterialProfile.Ecosystem ecosystem : MaterialProfile.Ecosystem.values()) {
            material.profile(ecosystem).ifPresent(profile ->
                    profiles.add(ecosystem.name() + "=" + profile.materialId()));
        }
        return new Inspection(
                material.canonicalId(),
                material.physicalTag(),
                List.copyOf(profiles),
                material.hasProfile(MaterialProfile.Ecosystem.SILENT_GEAR),
                material.hasProfile(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT));
    }

    public record Inspection(
            ResourceLocation canonicalId,
            ResourceLocation physicalTag,
            List<String> profiles,
            boolean silentGearPresent,
            boolean tinkersPresent) {

        private static Inspection missing(ResourceLocation id) {
            return new Inspection(id, null, List.of(), false, false);
        }

        public boolean bridgeCandidate() {
            return silentGearPresent != tinkersPresent;
        }

        public boolean correlated() {
            return silentGearPresent && tinkersPresent;
        }
    }
}
