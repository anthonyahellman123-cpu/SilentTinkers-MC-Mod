package com.anthonyahellman.silenttinkers.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Silent Gear's fully evaluated main-material stats at the instant of melting. */
public record AlloyStatSnapshot(
        float durability,
        float miningSpeed,
        float meleeDamage,
        float attackSpeed,
        ResourceLocation harvestTier) {
    private static final String DURABILITY = "Durability";
    private static final String MINING_SPEED = "MiningSpeed";
    private static final String MELEE_DAMAGE = "MeleeDamage";
    private static final String ATTACK_SPEED = "AttackSpeed";
    private static final String HARVEST_TIER = "HarvestTier";

    public AlloyStatSnapshot {
        requireFiniteNonNegative(durability, DURABILITY);
        requireFiniteNonNegative(miningSpeed, MINING_SPEED);
        requireFiniteNonNegative(meleeDamage, MELEE_DAMAGE);
        if (!Float.isFinite(attackSpeed)) {
            throw new IllegalArgumentException("AttackSpeed must be finite");
        }
        if (harvestTier == null) {
            throw new IllegalArgumentException("HarvestTier is required");
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(DURABILITY, durability);
        tag.putFloat(MINING_SPEED, miningSpeed);
        tag.putFloat(MELEE_DAMAGE, meleeDamage);
        tag.putFloat(ATTACK_SPEED, attackSpeed);
        tag.putString(HARVEST_TIER, harvestTier.toString());
        return tag;
    }

    public static Optional<AlloyStatSnapshot> load(CompoundTag tag) {
        if (!tag.contains(DURABILITY, Tag.TAG_ANY_NUMERIC)
                || !tag.contains(MINING_SPEED, Tag.TAG_ANY_NUMERIC)
                || !tag.contains(MELEE_DAMAGE, Tag.TAG_ANY_NUMERIC)
                || !tag.contains(ATTACK_SPEED, Tag.TAG_ANY_NUMERIC)) {
            return Optional.empty();
        }
        ResourceLocation tier = ResourceLocation.tryParse(tag.getString(HARVEST_TIER));
        if (tier == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new AlloyStatSnapshot(
                    tag.getFloat(DURABILITY), tag.getFloat(MINING_SPEED),
                    tag.getFloat(MELEE_DAMAGE), tag.getFloat(ATTACK_SPEED), tier));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void requireFiniteNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
