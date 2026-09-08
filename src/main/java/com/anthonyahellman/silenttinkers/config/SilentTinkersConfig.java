package com.anthonyahellman.silenttinkers.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SilentTinkersConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue PRIMARY_TRAIT_PERCENT;
    private static final ForgeConfigSpec.DoubleValue SECONDARY_TRAIT_PERCENT;
    private static final ForgeConfigSpec.DoubleValue FULL_TRAIT_PERCENT;
    private static final ForgeConfigSpec.DoubleValue DURABILITY_TRANSLATION_PERCENT;
    private static final ForgeConfigSpec.DoubleValue MINING_SPEED_TRANSLATION_PERCENT;
    private static final ForgeConfigSpec.DoubleValue MELEE_DAMAGE_TRANSLATION_PERCENT;
    private static final ForgeConfigSpec.DoubleValue ATTACK_SPEED_TRANSLATION_PERCENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                "Controls how much of a material must be present in a composite alloy",
                "before that material contributes its native Tinkers traits.",
                "Numeric stat weighting is independent of these trait gates.")
                .push("traits");

        PRIMARY_TRAIT_PERCENT = builder
                .comment("Minimum material percentage required for its primary traits. Range: 0-100.")
                .defineInRange("primaryTraitPercent", 25.0, 0.0, 100.0);
        SECONDARY_TRAIT_PERCENT = builder
                .comment("Minimum material percentage required for its primary and secondary traits. Range: 0-100.")
                .defineInRange("secondaryTraitPercent", 50.0, 0.0, 100.0);
        FULL_TRAIT_PERCENT = builder
                .comment("Minimum material percentage required for its complete trait package. Range: 0-100.")
                .defineInRange("fullTraitPercent", 75.0, 0.0, 100.0);
        builder.pop();

        builder.comment(
                "Global numeric translation percentages used when SilentTinkers bridges a material.",
                "100 preserves the source ecosystem's evaluated value; 80 carries 80% of it.",
                "These are server-side balance controls and do not affect native materials that already have compatibility.")
                .push("translation");
        DURABILITY_TRANSLATION_PERCENT = builder.defineInRange("durabilityPercent", 100.0, 0.0, 500.0);
        MINING_SPEED_TRANSLATION_PERCENT = builder.defineInRange("miningSpeedPercent", 100.0, 0.0, 500.0);
        MELEE_DAMAGE_TRANSLATION_PERCENT = builder.defineInRange("meleeDamagePercent", 100.0, 0.0, 500.0);
        ATTACK_SPEED_TRANSLATION_PERCENT = builder.defineInRange("attackSpeedPercent", 100.0, 0.0, 500.0);
        builder.pop();

        SPEC = builder.build();
    }

    private SilentTinkersConfig() {}

    public static TraitThresholds traitThresholds() {
        double primary = PRIMARY_TRAIT_PERCENT.get();
        double secondary = SECONDARY_TRAIT_PERCENT.get();
        double full = FULL_TRAIT_PERCENT.get();
        double low = Math.min(primary, Math.min(secondary, full));
        double high = Math.max(primary, Math.max(secondary, full));
        double middle = primary + secondary + full - low - high;
        return new TraitThresholds(low, middle, high);
    }

    public static TranslationPercentages translationPercentages() {
        return new TranslationPercentages(
                DURABILITY_TRANSLATION_PERCENT.get(),
                MINING_SPEED_TRANSLATION_PERCENT.get(),
                MELEE_DAMAGE_TRANSLATION_PERCENT.get(),
                ATTACK_SPEED_TRANSLATION_PERCENT.get());
    }

    public record TranslationPercentages(
            double durability,
            double miningSpeed,
            double meleeDamage,
            double attackSpeed) {
        public TranslationPercentages {
            requireFiniteNonNegative(durability, "durability");
            requireFiniteNonNegative(miningSpeed, "miningSpeed");
            requireFiniteNonNegative(meleeDamage, "meleeDamage");
            requireFiniteNonNegative(attackSpeed, "attackSpeed");
        }

        private static void requireFiniteNonNegative(double value, String name) {
            if (!Double.isFinite(value) || value < 0.0) {
                throw new IllegalArgumentException(name + " translation percentage must be finite and non-negative");
            }
        }
    }
}
