package com.anthonyahellman.silenttinkers.material;

/**
 * Last-resort balance policy for materials that neither Silent Gear nor
 * Tinkers has authored compatibility for.
 *
 * <p>These values are intentionally generic power brackets, not claims about
 * a material's identity. Dedicated compat data always wins. The planner UI can
 * also expose that a material is using a fallback so pack authors can override
 * it later.</p>
 */
public final class TierFallbackPolicy {
    private TierFallbackPolicy() {}

    public static FallbackProfile forTier(int miningTier) {
        int tier = Math.max(0, miningTier);

        // Vanilla-ish brackets provide readable defaults. Beyond tier 4 we
        // extrapolate gently instead of hardcoding an upper limit for modpacks.
        return switch (tier) {
            case 0 -> new FallbackProfile(tier, 0.70, 0.75, 0.90, Source.MINING_TIER);
            case 1 -> new FallbackProfile(tier, 0.85, 0.90, 0.95, Source.MINING_TIER);
            case 2 -> new FallbackProfile(tier, 1.00, 1.00, 1.00, Source.MINING_TIER);
            case 3 -> new FallbackProfile(tier, 1.25, 1.15, 1.05, Source.MINING_TIER);
            case 4 -> new FallbackProfile(tier, 1.50, 1.30, 1.10, Source.MINING_TIER);
            default -> {
                int aboveFour = tier - 4;
                yield new FallbackProfile(
                        tier,
                        1.50 + 0.20 * aboveFour,
                        1.30 + 0.15 * aboveFour,
                        1.10 + 0.05 * aboveFour,
                        Source.MINING_TIER);
            }
        };
    }

    /** Multipliers are consumed later by ecosystem-specific generators. */
    public record FallbackProfile(
            int miningTier,
            double durabilityMultiplier,
            double damageMultiplier,
            double speedMultiplier,
            Source source) {}

    public enum Source {
        MINING_TIER
    }
}
