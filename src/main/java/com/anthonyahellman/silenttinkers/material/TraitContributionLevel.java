package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.config.TraitThresholds;

import java.util.Objects;

/** Composition-derived strength, independent from how many traits a threshold exposes. */
public final class TraitContributionLevel {
    private static final double FULL_MATERIAL_PERCENT = 100.0;
    private static final double PERCENT_EPSILON = 0.000_001;

    private TraitContributionLevel() {}

    public static int forPercent(double materialPercent, TraitThresholds thresholds) {
        Objects.requireNonNull(thresholds, "thresholds");
        if (!Double.isFinite(materialPercent) || materialPercent < 0.0 || materialPercent > 100.0) {
            throw new IllegalArgumentException("materialPercent must be from 0 to 100");
        }
        if (materialPercent + PERCENT_EPSILON < thresholds.primaryPercent()) return 0;
        if (materialPercent + PERCENT_EPSILON < thresholds.secondaryPercent()) return 1;
        if (materialPercent + PERCENT_EPSILON < thresholds.fullPercent()) return 2;
        if (materialPercent + PERCENT_EPSILON < FULL_MATERIAL_PERCENT) return 3;
        return 4;
    }

}
