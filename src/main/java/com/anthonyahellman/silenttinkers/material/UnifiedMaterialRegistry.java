package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Proof-of-concept registry for correlating physical materials across Silent
 * Gear and Tinkers. It is deliberately tiny: Iron proves the model before we
 * add automatic discovery or alter the existing alloy pipeline.
 */
public final class UnifiedMaterialRegistry {
    private static final Map<ResourceLocation, UnifiedMaterial> MATERIALS = new LinkedHashMap<>();

    static {
        registerIronProof();
    }

    private UnifiedMaterialRegistry() {}

    private static void registerIronProof() {
        EnumMap<MaterialProfile.Ecosystem, MaterialProfile> profiles =
                new EnumMap<>(MaterialProfile.Ecosystem.class);

        profiles.put(MaterialProfile.Ecosystem.SILENT_GEAR,
                new MaterialProfile(
                        MaterialProfile.Ecosystem.SILENT_GEAR,
                        id("silentgear", "iron"),
                        java.util.List.of()));

        profiles.put(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT,
                new MaterialProfile(
                        MaterialProfile.Ecosystem.TINKERS_CONSTRUCT,
                        id("tconstruct", "iron"),
                        java.util.List.of()));

        register(new UnifiedMaterial(
                id("minecraft", "iron"),
                id("forge", "ingots/iron"),
                profiles));
    }

    public static void register(UnifiedMaterial material) {
        UnifiedMaterial previous = MATERIALS.putIfAbsent(material.canonicalId(), material);
        if (previous != null) {
            throw new IllegalStateException("Duplicate unified material: " + material.canonicalId());
        }
    }

    public static Optional<UnifiedMaterial> get(ResourceLocation canonicalId) {
        return Optional.ofNullable(MATERIALS.get(canonicalId));
    }

    public static Map<ResourceLocation, UnifiedMaterial> all() {
        return Map.copyOf(MATERIALS);
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
