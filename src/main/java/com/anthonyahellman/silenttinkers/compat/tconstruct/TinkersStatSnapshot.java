package com.anthonyahellman.silenttinkers.compat.tconstruct;

import net.minecraft.resources.ResourceLocation;

/**
 * Native Tinkers melee/harvest stats kept separate from ecosystem-neutral
 * translation because handle values are percentage modifiers, not absolute
 * Silent Gear-style stats.
 */
public record TinkersStatSnapshot(
        int headDurability,
        float headMiningSpeed,
        float headMeleeAttack,
        ResourceLocation harvestTier,
        float handleDurabilityModifier,
        float handleMiningSpeedModifier,
        float handleAttackSpeedModifier,
        float handleDamageModifier) {

    public TinkersStatSnapshot {
        if (headDurability < 0) throw new IllegalArgumentException("headDurability must be non-negative");
        requireFinite(headMiningSpeed, "headMiningSpeed");
        requireFinite(headMeleeAttack, "headMeleeAttack");
        requireFinite(handleDurabilityModifier, "handleDurabilityModifier");
        requireFinite(handleMiningSpeedModifier, "handleMiningSpeedModifier");
        requireFinite(handleAttackSpeedModifier, "handleAttackSpeedModifier");
        requireFinite(handleDamageModifier, "handleDamageModifier");
        if (harvestTier == null) throw new IllegalArgumentException("harvestTier is required");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
