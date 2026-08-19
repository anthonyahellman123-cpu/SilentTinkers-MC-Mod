package com.anthonyahellman.silenttinkers.config;

/** Pure percentage rules, kept separate from Forge config so they are easy to test. */
public record TraitThresholds(double primaryPercent, double secondaryPercent, double fullPercent) {
    public TraitThresholds {
        requirePercent(primaryPercent, "primaryPercent");
        requirePercent(secondaryPercent, "secondaryPercent");
        requirePercent(fullPercent, "fullPercent");
        if (primaryPercent > secondaryPercent || secondaryPercent > fullPercent) {
            throw new IllegalArgumentException("Trait thresholds must satisfy primary <= secondary <= full");
        }
    }

    public TraitAccess accessFor(double materialPercent) {
        requirePercent(materialPercent, "materialPercent");
        if (materialPercent >= fullPercent) {
            return TraitAccess.FULL;
        }
        if (materialPercent >= secondaryPercent) {
            return TraitAccess.SECONDARY;
        }
        if (materialPercent >= primaryPercent) {
            return TraitAccess.PRIMARY;
        }
        return TraitAccess.NONE;
    }

    private static void requirePercent(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException(name + " must be from 0 to 100");
        }
    }
}
