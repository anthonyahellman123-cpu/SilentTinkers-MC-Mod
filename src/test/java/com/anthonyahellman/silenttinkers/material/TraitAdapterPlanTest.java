package com.anthonyahellman.silenttinkers.material;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TraitAdapterPlanTest {
    @Test
    void pixieUsesCentralSupportedAdapterAtCompositionLevel() {
        for (int level = 1; level <= 4; level++) {
            TraitAdapterPlan.Decision decision = TraitAdapterPlan.create(
                    List.of(TraitAdapterPlan.PIXIE_TRAIT), level).get(0);

            assertEquals(TraitAdapterPlan.Status.SUPPORTED, decision.status());
            assertEquals(level, decision.contributionLevel());
            assertEquals(TraitAdapterPlan.PIXIE_MODIFIER, decision.targetModifier().orElseThrow());
        }
    }

    @Test
    void unknownBehaviorDoesNotBlockOrFabricateAnAdapter() {
        TraitAdapterPlan.Decision decision = TraitAdapterPlan.create(
                List.of(id("destiny:paracausal_burst")), 2).get(0);

        assertEquals(TraitAdapterPlan.Status.UNSUPPORTED, decision.status());
        assertTrue(decision.targetModifier().isEmpty());
    }

    @Test
    void verifiedDirectMagneticMappingUsesCompositionLevel() {
        TraitAdapterPlan.Decision decision = TraitAdapterPlan.create(
                List.of(TraitAdapterPlan.MAGNETIC_TRAIT), 2).get(0);

        assertEquals(TraitAdapterPlan.Status.SUPPORTED, decision.status());
        assertEquals(TraitAdapterPlan.Kind.DIRECT, decision.kind());
        assertEquals(TraitAdapterPlan.MAGNETIC_MODIFIER, decision.targetModifier().orElseThrow());
        assertEquals(2, decision.contributionLevel());
    }

    @Test
    void knownBehaviorWithoutAdapterIsExplicitlyUnsupported() {
        TraitAdapterPlan.Decision decision = TraitAdapterPlan.create(
                List.of(TraitAdapterPlan.WITHER_SKULL_TRAIT), 3).get(0);

        assertEquals(TraitAdapterPlan.Status.UNSUPPORTED, decision.status());
        assertEquals(TraitAdapterPlan.Kind.BEHAVIORAL, decision.kind());
        assertTrue(decision.targetModifier().isEmpty());
    }

    @Test
    void unavailableModifierTargetFailsClosed() {
        TraitAdapterPlan.Decision decision = TraitAdapterPlan.create(
                List.of(TraitAdapterPlan.MAGNETIC_TRAIT), 2, ignored -> false).get(0);

        assertEquals(TraitAdapterPlan.Status.TARGET_UNAVAILABLE, decision.status());
        assertTrue(decision.targetModifier().isEmpty());
    }

    @Test
    void duplicateSourceTraitDoesNotDuplicateApplication() {
        List<TraitAdapterPlan.Decision> decisions = TraitAdapterPlan.create(
                List.of(TraitAdapterPlan.MAGNETIC_TRAIT, TraitAdapterPlan.MAGNETIC_TRAIT), 2);

        assertEquals(1, decisions.size());
    }

    @Test
    void equivalentTargetsCollapseToHighestLevelIndependentOfOrder() {
        List<TraitAdapterPlan.Decision> lowerFirst = List.of(
                TraitAdapterPlan.create(List.of(TraitAdapterPlan.MAGNETIC_TRAIT), 1).get(0),
                TraitAdapterPlan.create(List.of(TraitAdapterPlan.MAGNETIC_TRAIT), 3).get(0));
        List<TraitAdapterPlan.Decision> higherFirst = List.of(lowerFirst.get(1), lowerFirst.get(0));

        Map<net.minecraft.resources.ResourceLocation, Integer> expected =
                Map.of(TraitAdapterPlan.MAGNETIC_MODIFIER, 3);
        assertEquals(expected, TraitAdapterPlan.applications(lowerFirst, List.of()));
        assertEquals(expected, TraitAdapterPlan.applications(higherFirst, List.of()));
        assertTrue(TraitAdapterPlan.applications(lowerFirst,
                List.of(TraitAdapterPlan.MAGNETIC_MODIFIER)).isEmpty());
    }

    @Test
    void belowThresholdTraitDoesNotProduceModifier() {
        TraitAdapterPlan.Decision decision = TraitAdapterPlan.create(
                List.of(TraitAdapterPlan.PIXIE_TRAIT), 0).get(0);

        assertEquals(TraitAdapterPlan.Status.BELOW_THRESHOLD, decision.status());
        assertTrue(decision.targetModifier().isEmpty());
    }

    @Test
    void fourQuarterMaterialsEachPlanAnIndependentLevelOneAdapter() {
        Map<net.minecraft.resources.ResourceLocation, Long> raw = new LinkedHashMap<>();
        raw.put(id("silentgear:iron"), 25L);
        raw.put(id("silentcompat:elementium"), 25L);
        raw.put(id("tinkers_advanced:antimony"), 25L);
        raw.put(id("iceandfire:dragonsteel_fire"), 25L);
        AlloyComposition composition = AlloyComposition.of(raw);

        List<TraitAdapterPlan.Decision> decisions = composition.ingredients().stream()
                .map(ingredient -> 100.0 * ingredient.units() / composition.totalUnits())
                .map(percent -> TraitContributionLevel.forPercent(percent,
                        new com.anthonyahellman.silenttinkers.config.TraitThresholds(25, 50, 75)))
                .flatMap(level -> TraitAdapterPlan.create(List.of(TraitAdapterPlan.PIXIE_TRAIT), level).stream())
                .toList();

        assertEquals(4, decisions.size());
        assertTrue(decisions.stream().allMatch(decision -> decision.contributionLevel() == 1));
        assertTrue(decisions.stream().allMatch(decision -> decision.status() == TraitAdapterPlan.Status.SUPPORTED));
    }

    @Test
    void repeatedAdapterProcessingIsStable() {
        List<TraitAdapterPlan.Decision> first = TraitAdapterPlan.create(
                List.of(TraitAdapterPlan.PIXIE_TRAIT, id("destiny:paracausal_burst")), 3);
        assertEquals(first, TraitAdapterPlan.create(
                List.of(TraitAdapterPlan.PIXIE_TRAIT, id("destiny:paracausal_burst")), 3));
    }

    private static net.minecraft.resources.ResourceLocation id(String value) {
        return new net.minecraft.resources.ResourceLocation(value);
    }
}
