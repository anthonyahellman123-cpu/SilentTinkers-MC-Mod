package com.anthonyahellman.silenttinkers.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraitThresholdsTest {
    @Test
    void defaultsAllowThreeWayPrimaryTraits() {
        TraitThresholds thresholds = new TraitThresholds(25, 50, 75);

        assertEquals(TraitAccess.NONE, thresholds.accessFor(10));
        assertEquals(TraitAccess.PRIMARY, thresholds.accessFor(33.333));
        assertEquals(TraitAccess.SECONDARY, thresholds.accessFor(60));
        assertEquals(TraitAccess.FULL, thresholds.accessFor(80));
    }

    @Test
    void zeroThresholdsReleaseThePest() {
        TraitThresholds thresholds = new TraitThresholds(0, 0, 0);

        assertEquals(TraitAccess.FULL, thresholds.accessFor(0.001));
    }

    @Test
    void requiresOrderedPercentages() {
        assertThrows(IllegalArgumentException.class, () -> new TraitThresholds(50, 25, 75));
    }
}
