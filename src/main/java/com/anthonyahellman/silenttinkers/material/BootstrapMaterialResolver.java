package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

/** Single entry point for resolving unknown-material bootstrap stats. */
public final class BootstrapMaterialResolver {
    private BootstrapMaterialResolver() {}

    public static BootstrapMaterialProfile resolve(ResourceLocation physicalItem, int detectedMiningTier) {
        return BootstrapMaterialProfile.resolve(
                physicalItem,
                detectedMiningTier,
                MaterialOverrideRegistry.find(physicalItem));
    }
}
