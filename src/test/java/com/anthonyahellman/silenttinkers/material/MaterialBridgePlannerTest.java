package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearDiscoveryBridge;
import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersCorrelationAdapter;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MaterialBridgePlannerTest {
    @Test
    void oneSidedAliasDoesNotBridgeMaterialAlreadyCorrelatedElsewhere() {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();
        MaterialProfile sg = profile(MaterialProfile.Ecosystem.SILENT_GEAR, "silentcompat:bronze");
        MaterialProfile tc = profile(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, "tconstruct:bronze");

        // The ingot proves real native compatibility. A block alias exists only
        // on Silent Gear and must not produce a duplicate bridge plan.
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

        // Looks attractive to canonical selection because of its name but is
        // multi-claimed on Tinkers and therefore quarantined.
        index.accept(id("test:ingot_ambiguous"), sg);
        index.accept(id("test:ingot_ambiguous"), tcWood);
        index.accept(id("test:ingot_ambiguous"), tcBamboo);

        List<MaterialBridgePlan> plans = MaterialBridgePlanner.plan(snapshot(index));

        assertEquals(1, plans.size());
        assertEquals(MaterialBridgePlan.Action.PRESERVE, plans.get(0).action());
        assertEquals(id("test:wood_item"), plans.get(0).physicalItem());
        assertFalse(plans.stream().anyMatch(plan -> plan.physicalItem().equals(id("test:ingot_ambiguous"))));
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
