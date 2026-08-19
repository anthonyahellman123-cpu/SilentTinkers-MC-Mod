package com.anthonyahellman.silenttinkers.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SilentTinkersConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue PRIMARY_TRAIT_PERCENT;
    private static final ForgeConfigSpec.DoubleValue SECONDARY_TRAIT_PERCENT;
    private static final ForgeConfigSpec.DoubleValue FULL_TRAIT_PERCENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                "Controls how much of a material must be present in a composite alloy",
                "before that material contributes its native Tinkers traits.",
                "Numeric stat weighting is independent of these trait gates.",
                "",
                "Examples using the defaults:",
                "  10% material: numeric stats only",
                "  33% material: primary traits",
                "  60% material: primary and secondary traits",
                "  80% material: complete native trait package",
                "",
                "Set all three values to 0 to grant every present material its full package.",
                "Set all three values to 100 to require a pure material for any traits.")
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
        SPEC = builder.build();
    }

    private SilentTinkersConfig() {}

    public static TraitThresholds traitThresholds() {
        double primary = PRIMARY_TRAIT_PERCENT.get();
        double secondary = SECONDARY_TRAIT_PERCENT.get();
        double full = FULL_TRAIT_PERCENT.get();
        // Keep the server bootable if a user manually enters thresholds out of
        // order. Sorting preserves all three chosen values and makes behavior deterministic.
        double low = Math.min(primary, Math.min(secondary, full));
        double high = Math.max(primary, Math.max(secondary, full));
        double middle = primary + secondary + full - low - high;
        return new TraitThresholds(low, middle, high);
    }
}
