package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

/**
 * Ecosystem-neutral tool statistics used at the translation boundary.
 * Discovery identifies materials; readers evaluate native stats; translators
 * normalize them here; target generators consume this record.
 */
public record TranslatedMaterialStats(
        float durability,
        float miningSpeed,
        float meleeDamage,
        float attackSpeed,
        ResourceLocation harvestTier) {

    public TranslatedMaterialStats {
        requireFiniteNonNegative(durability, "durability");
        requireFiniteNonNegative(miningSpeed, "miningSpeed");
        requireFiniteNonNegative(meleeDamage, "meleeDamage");
        if (!Float.isFinite(attackSpeed)) throw new IllegalArgumentException("attackSpeed must be finite");
        if (harvestTier == null) throw new IllegalArgumentException("harvestTier is required");
    }

    public static TranslatedMaterialStats fromSilentGear(AlloyStatSnapshot snapshot) {
        return new TranslatedMaterialStats(snapshot.durability(), snapshot.miningSpeed(), snapshot.meleeDamage(),
                snapshot.attackSpeed(), snapshot.harvestTier());
    }

    private static void requireFiniteNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException(name + " must be finite and non-negative");
    }
}
