package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;

/** Applies server balance controls after native source stats are evaluated. */
public final class MaterialTranslationPolicy {
    private MaterialTranslationPolicy() {}

    public static TranslatedMaterialStats apply(TranslatedMaterialStats source) {
        SilentTinkersConfig.TranslationPercentages percentages = SilentTinkersConfig.translationPercentages();
        return new TranslatedMaterialStats(
                scale(source.durability(), percentages.durability()),
                scale(source.miningSpeed(), percentages.miningSpeed()),
                scale(source.meleeDamage(), percentages.meleeDamage()),
                scale(source.attackSpeed(), percentages.attackSpeed()),
                source.harvestTier());
    }

    private static float scale(float value, double percent) {
        double scaled = value * (percent / 100.0);
        if (!Double.isFinite(scaled) || scaled > Float.MAX_VALUE) {
            throw new IllegalArgumentException("Translated stat overflow: " + scaled);
        }
        return (float) scaled;
    }
}
