package com.anthonyahellman.silenttinkers.material;

import java.util.Objects;

/** Pure guard preventing non-tool material roles from masquerading as usable Tinkers heads. */
public final class HeadMaterialEligibility {
    private HeadMaterialEligibility() {}

    public static Result evaluate(TranslatedMaterialStats stats) {
        Objects.requireNonNull(stats, "stats");
        if (stats.durability() <= 0.0f) {
            return new Result(false, "no positive main-part durability");
        }
        if (stats.miningSpeed() <= 0.0f && stats.meleeDamage() <= 0.0f) {
            return new Result(false, "no positive mining-speed or melee-damage contribution");
        }
        return new Result(true, "main-part statistics are usable");
    }

    public record Result(boolean eligible, String detail) {
        public Result {
            Objects.requireNonNull(detail, "detail");
        }
    }
}
