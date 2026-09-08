package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.config.TraitAccess;
import com.anthonyahellman.silenttinkers.config.TraitThresholds;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TraitForwardingPlanTest {
    private static final TraitThresholds THRESHOLDS = new TraitThresholds(25, 50, 75);

    @Test
    void selectionPreservesDeclaredOrderCountsAndLevels() {
        List<TraitToken> traits = List.of(
                trait("test:first", 2), trait("test:second", 1), trait("test:third", 4));

        assertTrue(TraitForwardingPlan.select(TraitAccess.NONE, traits).isEmpty());
        assertEquals(List.of(traits.get(0)),
                TraitForwardingPlan.select(TraitAccess.PRIMARY, traits));
        assertEquals(traits.subList(0, 2),
                TraitForwardingPlan.select(TraitAccess.SECONDARY, traits));
        assertEquals(traits, TraitForwardingPlan.select(TraitAccess.FULL, traits));
        assertTrue(TraitForwardingPlan.select(TraitAccess.FULL, List.of()).isEmpty());
        assertEquals(List.of(traits.get(0)),
                TraitForwardingPlan.select(TraitAccess.FULL, List.of(traits.get(0))));
        assertEquals(1, TraitForwardingPlan.select(
                TraitAccess.SECONDARY, List.of(traits.get(0))).size());
        assertEquals(2, TraitForwardingPlan.select(
                TraitAccess.FULL, traits.subList(0, 2)).size());
    }

    @Test
    void fourQuarterMaterialsEachReceiveIndependentPrimaryEligibility() {
        AlloyComposition composition = fourWay(false);
        List<TraitForwardingPlan.Decision<TraitToken>> decisions = create(composition);

        assertEquals(4, decisions.size());
        for (TraitForwardingPlan.Decision<TraitToken> decision : decisions) {
            assertEquals(25.0, decision.materialPercent(), 0.000_001);
            assertEquals(TraitAccess.PRIMARY, decision.access());
            assertEquals(3, decision.availableTraitCount());
            assertEquals(1, decision.forwardedTraits().size());
            assertEquals(decision.sourceMaterialId().getPath(),
                    decision.forwardedTraits().get(0).modifierId().getPath());
        }
    }

    @Test
    void insertionOrderAndRepeatedProcessingProduceSamePlan() {
        List<TraitForwardingPlan.Decision<TraitToken>> forward = create(fourWay(false));
        List<TraitForwardingPlan.Decision<TraitToken>> reversed = create(fourWay(true));
        List<TraitForwardingPlan.Decision<TraitToken>> repeated = create(fourWay(false));

        assertEquals(forward, reversed);
        assertEquals(forward, repeated);
    }

    @Test
    void equivalentModifierEntriesRemainSeparateWithTheirLegitimateLevels() {
        AlloyComposition composition = AlloyComposition.of(Map.of(
                id("alpha:first"), 1L,
                id("beta:second"), 1L));
        Map<ResourceLocation, List<TraitToken>> traits = Map.of(
                id("alpha:first"), List.of(trait("shared:resonance", 1), trait("alpha:unique", 2)),
                id("beta:second"), List.of(trait("shared:resonance", 3), trait("beta:unique", 1)));

        List<TraitToken> forwarded = TraitForwardingPlan.create(
                        composition, THRESHOLDS,
                        source -> TraitSourceResolver.resolve(source, traits.keySet()),
                        traits::get).stream()
                .flatMap(decision -> decision.forwardedTraits().stream())
                .toList();

        assertEquals(List.of(
                trait("shared:resonance", 1), trait("alpha:unique", 2),
                trait("shared:resonance", 3), trait("beta:unique", 1)), forwarded);
    }

    @Test
    void ambiguousSourceRefusesTraitsWithoutCallingLookup() {
        AtomicBoolean lookupCalled = new AtomicBoolean();
        ResourceLocation source = id("silentcompat:paracausal_alloy");
        AlloyComposition composition = AlloyComposition.of(Map.of(source, 1L));

        TraitForwardingPlan.Decision<TraitToken> decision = TraitForwardingPlan.create(
                composition, THRESHOLDS,
                ignored -> TraitSourceResolver.resolve(source,
                        List.of(id("alpha:paracausal_alloy"), id("beta:paracausal_alloy"))),
                ignored -> {
                    lookupCalled.set(true);
                    return List.of(trait("test:wrong", 1));
                }).get(0);

        assertEquals(TraitSourceResolver.Status.AMBIGUOUS,
                decision.resolution().orElseThrow().status());
        assertTrue(decision.forwardedTraits().isEmpty());
        assertFalse(lookupCalled.get());
    }

    private static List<TraitForwardingPlan.Decision<TraitToken>> create(AlloyComposition composition) {
        List<ResourceLocation> ids = composition.ingredients().stream()
                .map(MaterialIngredient::materialId)
                .toList();
        return TraitForwardingPlan.create(
                composition, THRESHOLDS,
                source -> TraitSourceResolver.resolve(source, ids),
                source -> List.of(
                        new TraitToken(new ResourceLocation("trait", source.getPath()), 1),
                        trait("trait:secondary", 2),
                        trait("trait:full", 3)));
    }

    private static AlloyComposition fourWay(boolean reverse) {
        List<ResourceLocation> ids = new ArrayList<>(List.of(
                id("silentgear:iron"),
                id("silentcompat:elementium"),
                id("tinkers_advanced:antimony"),
                id("tcompat:calorite")));
        if (reverse) Collections.reverse(ids);
        Map<ResourceLocation, Long> raw = new LinkedHashMap<>();
        ids.forEach(id -> raw.put(id, 25L));
        return AlloyComposition.of(raw);
    }

    private static TraitToken trait(String id, int level) {
        return new TraitToken(TraitForwardingPlanTest.id(id), level);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private record TraitToken(ResourceLocation modifierId, int level) {}
}
