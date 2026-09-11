package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.config.TraitThresholds;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    void requestedCompositeMatrixUsesNormalizedCompositionPercentages() {
        assertEquals(List.of(4), levels(100));
        assertEquals(List.of(2, 2), levels(50, 50));
        assertEquals(List.of(3, 1), levels(75, 25));
        assertEquals(List.of(1, 1, 1), levels(1, 1, 1));
        assertEquals(List.of(2, 1, 1), levels(2, 1, 1));
        assertEquals(List.of(1, 1, 1, 1), levels(25, 25, 25, 25));
        assertEquals(List.of(1, 1, 0, 0), levels(40, 30, 20, 10));
    }

    @Test
    void imperfectButRecoverableTotalsNormalizeDeterministically() {
        assertEquals(List.of(2, 1, 0, 0), levels(51, 26, 13, 10));
    }

    private static List<Integer> levels(long... units) {
        Map<ResourceLocation, Long> raw = new LinkedHashMap<>();
        for (int index = 0; index < units.length; index++) {
            raw.put(new ResourceLocation("test", "material_" + index), units[index]);
        }
        AlloyComposition composition = AlloyComposition.of(raw);
        return composition.ingredients().stream()
                .map(ingredient -> 100.0 * ingredient.units() / composition.totalUnits())
                .map(percent -> TraitContributionLevel.forPercent(percent, THRESHOLDS))
                .toList();
    }
}
