package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Optional authored tuning for a specific physical material.
 *
 * <p>Overrides sit above mining-tier fallback but below native compatibility:
 * if a mod already provides proper ecosystem data, SilentTinkers preserves it.
 * Empty fields inherit the generated fallback, so pack authors can tune one
 * stat without having to redefine the whole material.</p>
 */
public record MaterialStatOverride(
        ResourceLocation physicalItem,
        OptionalInt miningTier,
        OptionalDouble durabilityMultiplier,
        OptionalDouble damageMultiplier,
        OptionalDouble speedMultiplier) {

    public MaterialStatOverride {
        Objects.requireNonNull(physicalItem, "physicalItem");
        miningTier = Objects.requireNonNull(miningTier, "miningTier");
        durabilityMultiplier = Objects.requireNonNull(durabilityMultiplier, "durabilityMultiplier");
        damageMultiplier = Objects.requireNonNull(damageMultiplier, "damageMultiplier");
        speedMultiplier = Objects.requireNonNull(speedMultiplier, "speedMultiplier");
    }
}
