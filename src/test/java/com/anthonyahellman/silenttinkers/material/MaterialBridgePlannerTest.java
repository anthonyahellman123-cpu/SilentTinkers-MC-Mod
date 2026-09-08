package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearDiscoveryBridge;
import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersCorrelationAdapter;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialBridgePlannerTest {
    @Test
    void oneSidedAliasDoesNotBridgeMaterialAlreadyCorrelatedElsewhere() {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();
        MaterialProfile sg = profile(MaterialProfile.Ecosystem.SILENT_GEAR, "silentcompat:bronze");
        MaterialProfile tc = profile(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, "tconstruct:bronze");

        index.accept(id("test:bronze_ingot"), sg);
        index.accept(id("test:bronze_ingot"), tc);
        index.accept(id("test:bronze_block"), sg);

        List<MaterialBridgePlan> plans = MaterialBridgePlanner.plan(snapshot(index));

        assertEquals(1, plans.size());
        assertEquals(MaterialBridgePlan.Action.PRESERVE, plans.get(0).action());
        assertEquals(id("test:bronze_ingot"), plans.get(0).physicalItem());
    }

    @Test
    void ambiguousAliasCannotBecomeCanonicalRepresentative() {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();
        MaterialProfile sg = profile(MaterialProfile.Ecosystem.SILENT_GEAR, "silentgear:wood");
        MaterialProfile tcWood = profile(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, "tconstruct:wood");
        MaterialProfile tcBamboo = profile(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, "tconstruct:bamboo");

        index.accept(id("test:wood_item"), sg);
        index.accept(id("test:wood_item"), tcWood);
        index.accept(id("test:ingot_ambiguous"), sg);
        index.accept(id("test:ingot_ambiguous"), tcWood);
        index.accept(id("test:ingot_ambiguous"), tcBamboo);

        List<MaterialBridgePlan> plans = MaterialBridgePlanner.plan(snapshot(index));

        assertEquals(1, plans.size());
        assertEquals(MaterialBridgePlan.Action.PRESERVE, plans.get(0).action());
        assertEquals(id("test:wood_item"), plans.get(0).physicalItem());
        assertFalse(plans.stream().anyMatch(plan -> plan.physicalItem().equals(id("test:ingot_ambiguous"))));
    }

    @Test
    void bridgePlanCarriesSourceIdentityWithoutReverseAliasLookup() {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();
        MaterialProfile sg = profile(MaterialProfile.Ecosystem.SILENT_GEAR, "silentcompat:elementium");

        // Multiple aliases exercise canonical selection. The plan must still
        // carry the authoritative material ID directly from discovery.
        index.accept(id("botania:elementium_block"), sg);
        index.accept(id("botania:elementium_ingot"), sg);

        List<MaterialBridgePlan> plans = MaterialBridgePlanner.plan(snapshot(index));

        assertEquals(1, plans.size());
        MaterialBridgePlan plan = plans.get(0);
        assertEquals(MaterialBridgePlan.Action.BRIDGE, plan.action());
        assertEquals(id("botania:elementium_ingot"), plan.physicalItem());
        assertEquals(MaterialProfile.Ecosystem.SILENT_GEAR, plan.source().orElseThrow());
        assertEquals(id("silentcompat:elementium"), plan.sourceMaterialId().orElseThrow());
        assertEquals(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, plan.target().orElseThrow());
        assertEquals(id("silenttinkers:generated/tinkers_construct/silentcompat/elementium"),
                plan.targetMaterialId().orElseThrow());
    }

    @Test
    void oneSidedGeneratedTargetCannotFeedAnotherGenerationCycle() {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();
        ResourceLocation source = id("tinkers_advanced:neutronium");
        ResourceLocation generated = GeneratedMaterialOwnership.idFor(
                MaterialProfile.Ecosystem.SILENT_GEAR, source);

        index.accept(id("silenttinkers:generated_neutronium_ingot"),
                new MaterialProfile(MaterialProfile.Ecosystem.SILENT_GEAR, generated, List.of()));

        assertTrue(MaterialBridgePlanner.plan(snapshot(index)).isEmpty());
    }

    private static UnifiedMaterialDiscovery.Snapshot snapshot(MaterialCorrelationIndex index) {
        List<MaterialCorrelationIndex.Candidate> correlated = new ArrayList<>();
        List<MaterialCorrelationIndex.Candidate> bridge = new ArrayList<>();
        for (MaterialCorrelationIndex.Candidate candidate : index.all()) {
            if (candidate.correlated()) correlated.add(candidate);
            else if (candidate.bridgeCandidate()) bridge.add(candidate);
        }
        return new UnifiedMaterialDiscovery.Snapshot(
                index,
                new SilentGearDiscoveryBridge.DiscoveryReport(0, 0, 0),
                new TinkersCorrelationAdapter.DiscoveryReport(0, 0, 0),
                List.copyOf(correlated),
                List.copyOf(bridge),
                List.of());
    }

    private static MaterialProfile profile(MaterialProfile.Ecosystem ecosystem, String materialId) {
        return new MaterialProfile(ecosystem, id(materialId), List.of());
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
