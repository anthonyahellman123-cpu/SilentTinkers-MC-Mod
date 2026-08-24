package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Resolved ecosystem-neutral stats used when bootstrapping an unknown material. */
public record BootstrapMaterialProfile(
        ResourceLocation physicalItem,
        int miningTier,
        double durabilityMultiplier,
        double damageMultiplier,
        double speedMultiplier,
        Source source) {

    public static BootstrapMaterialProfile resolve(
            ResourceLocation item,
            int detectedMiningTier,
            Optional<MaterialStatOverride> override) {

        int tier = override.flatMap(o -> o.miningTier().isPresent()
                        ? Optional.of(o.miningTier().getAsInt())
                        : Optional.empty())
                .orElse(detectedMiningTier);

        TierFallbackPolicy.FallbackProfile fallback = TierFallbackPolicy.forTier(tier);

        double durability = override.filter(o -> o.durabilityMultiplier().isPresent())
                .map(o -> o.durabilityMultiplier().getAsDouble())
                .orElse(fallback.durabilityMultiplier());
        double damage = override.filter(o -> o.damageMultiplier().isPresent())
                .map(o -> o.damageMultiplier().getAsDouble())
                .orElse(fallback.damageMultiplier());
        double speed = override.filter(o -> o.speedMultiplier().isPresent())
                .map(o -> o.speedMultiplier().getAsDouble())
                .orElse(fallback.speedMultiplier());

        boolean authored = override.isPresent();
        return new BootstrapMaterialProfile(
                item, tier, durability, damage, speed,
                authored ? Source.AUTHORED_OVERRIDE : Source.MINING_TIER_FALLBACK);
    }

    public enum Source {
        MINING_TIER_FALLBACK,
        AUTHORED_OVERRIDE
    }
}
