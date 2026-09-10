package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.config.TraitThresholds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TraitContributionLevelTest {
    private static final TraitThresholds THRESHOLDS = new TraitThresholds(25, 50, 75);

    @Test
    void canonicalBoundariesProduceNoneThroughLevelFour() {
        assertEquals(0, TraitContributionLevel.forPercent(24.999, THRESHOLDS));
        assertEquals(1, TraitContributionLevel.forPercent(25.0, THRESHOLDS));
        assertEquals(2, TraitContributionLevel.forPercent(50.0, THRESHOLDS));
        assertEquals(3, TraitContributionLevel.forPercent(75.0, THRESHOLDS));
        assertEquals(4, TraitContributionLevel.forPercent(100.0, THRESHOLDS));
    }

    @Test
    void intermediateValuesRemainInTheirDeterministicBand() {
        assertEquals(1, TraitContributionLevel.forPercent(49.999, THRESHOLDS));
        assertEquals(2, TraitContributionLevel.forPercent(74.999, THRESHOLDS));
        assertEquals(3, TraitContributionLevel.forPercent(99.999, THRESHOLDS));
    }
}
