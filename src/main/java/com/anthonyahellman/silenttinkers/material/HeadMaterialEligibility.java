package com.anthonyahellman.silenttinkers.material;

import java.util.Objects;

/** Pure guard preventing non-tool material roles from masquerading as usable Tinkers heads. */
public final class HeadMaterialEligibility {
    private HeadMaterialEligibility() {}

    public static Result evaluate(TranslatedMaterialStats stats) {
        Objects.requireNonNull(stats, "stats");
        if (stats.durability() <= 0.0f) {
            return new Result(Classification.REJECTED, "no positive main-part durability");
        }
        if (stats.miningSpeed() <= 0.0f && stats.meleeDamage() <= 0.0f) {
            return new Result(Classification.ROLE_LIMITED,
                    "durable material has no mining-speed or melee-damage contribution");
        }
        return new Result(Classification.ACCEPTED, "main-part statistics are usable");
    }

    public record Result(Classification classification, String detail) {
        public Result {
            Objects.requireNonNull(classification, "classification");
            Objects.requireNonNull(detail, "detail");
        }

        public boolean eligible() {
            return classification == Classification.ACCEPTED;
        }
    }

    public enum Classification {
        ACCEPTED,
        ROLE_LIMITED,
        REJECTED
    }
}
