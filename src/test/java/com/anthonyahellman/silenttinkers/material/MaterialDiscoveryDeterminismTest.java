package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearDiscoveryBridge;
import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersCorrelationAdapter;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MaterialDiscoveryDeterminismTest {
    @AfterEach
    void clearRuntimeState() {
        MaterialDiscoveryState.clear();
    }

    @Test
    void repeatedEquivalentScansProduceStableCountsPlansAndFingerprint() {
        ScanResult first = scan(false);
        ScanResult second = scan(true);
        ScanResult third = scan(false);

        assertEquals(first, second);
        assertEquals(first, third);
        assertEquals(5, first.candidateCount());
        assertEquals(1, first.correlatedCount());
        assertEquals(3, first.bridgeCandidateCount());
        assertEquals(3, first.plans().size());
    }

    @Test
    void duplicateAndAmbiguousClaimsCannotHydraBridgePlans() {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();
        MaterialProfile sgBronze = profile(MaterialProfile.Ecosystem.SILENT_GEAR, "silentcompat:bronze");
        MaterialProfile tcBronze = profile(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, "tconstruct:bronze");
        MaterialProfile tcOther = profile(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, "addon:other_bronze");

        index.accept(id("example:bronze_ingot"), sgBronze);
        index.accept(id("example:bronze_ingot"), sgBronze);
        index.accept(id("example:bronze_ingot"), tcBronze);
        index.accept(id("example:bronze_block"), sgBronze);
        index.accept(id("example:ambiguous_bronze_ingot"), sgBronze);
        index.accept(id("example:ambiguous_bronze_ingot"), tcBronze);
        index.accept(id("example:ambiguous_bronze_ingot"), tcOther);

        UnifiedMaterialDiscovery.Snapshot snapshot = snapshot(index);
        List<MaterialBridgePlan> plans = MaterialBridgePlanner.plan(snapshot);

        assertEquals(1, plans.size());
        assertEquals(MaterialBridgePlan.Action.PRESERVE, plans.get(0).action());
        assertEquals(id("example:bronze_ingot"), plans.get(0).physicalItem());
        assertTrue(index.get(id("example:ambiguous_bronze_ingot")).orElseThrow().ambiguous());
        assertFalse(plans.stream().anyMatch(plan -> plan.physicalItem()
                .equals(id("example:ambiguous_bronze_ingot"))));
    }

    @Test
    void runtimeKeysAreStableAndDuplicateKeysAreQuarantined() {
        MaterialGenerationEvaluation mythril = ready("example:mythril_ingot", "silentgear:mythril", 900f);
        MaterialGenerationEvaluation adamant = ready("example:adamant_ingot", "silentgear:adamant", 1200f);
        UnifiedMaterialDiscovery.Snapshot ordered = evaluatedSnapshot(List.of(mythril, adamant));
        UnifiedMaterialDiscovery.Snapshot reversed = evaluatedSnapshot(List.of(adamant, mythril));

        MaterialDiscoveryState.publish(ordered);
        assertEquals(2, MaterialDiscoveryState.readyForTinkersCount());
        assertTrue(MaterialDiscoveryState.readyForTinkers(id("example:mythril_ingot")).isPresent());

        MaterialDiscoveryState.publish(reversed);
        assertEquals(2, MaterialDiscoveryState.readyForTinkersCount());
        assertEquals(mythril, MaterialDiscoveryState.readyForTinkers(id("example:mythril_ingot")).orElseThrow());

        MaterialGenerationEvaluation conflicting = ready(
                "example:mythril_ingot", "other:mythril", 901f);
        MaterialDiscoveryState.publish(evaluatedSnapshot(List.of(mythril, conflicting, adamant)));
        assertEquals(1, MaterialDiscoveryState.readyForTinkersCount());
        assertTrue(MaterialDiscoveryState.readyForTinkers(id("example:mythril_ingot")).isEmpty());
        assertTrue(MaterialDiscoveryState.readyForTinkers(id("example:adamant_ingot")).isPresent());
    }

    private static ScanResult scan(boolean reverseInput) {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();
        List<Claim> claims = new ArrayList<>(List.of(
                new Claim("example:bronze_ingot", MaterialProfile.Ecosystem.SILENT_GEAR, "silentcompat:bronze"),
                new Claim("example:bronze_ingot", MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, "tconstruct:bronze"),
                new Claim("example:bronze_block", MaterialProfile.Ecosystem.SILENT_GEAR, "silentcompat:bronze"),
                new Claim("example:ruby_gem", MaterialProfile.Ecosystem.SILENT_GEAR, "silentgear:ruby"),
                new Claim("example:ambiguous_ingot", MaterialProfile.Ecosystem.SILENT_GEAR, "silentgear:alpha"),
                new Claim("example:ambiguous_ingot", MaterialProfile.Ecosystem.SILENT_GEAR, "silentgear:beta"),
                new Claim("example:unique_ingot", MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, "addon:unique")));
        if (reverseInput) Collections.reverse(claims);
        for (Claim claim : claims) {
            index.accept(id(claim.physicalItem()), profile(claim.ecosystem(), claim.materialId()));
        }

        UnifiedMaterialDiscovery.Snapshot snapshot = snapshot(index);
        List<MaterialBridgePlan> plans = MaterialBridgePlanner.plan(snapshot);
        List<MaterialGenerationEvaluation> evaluations = plans.stream()
                .map(MaterialDiscoveryDeterminismTest::stableEvaluation)
                .toList();
        return new ScanResult(index.all().size(), snapshot.correlatedPhysicalItems(),
                snapshot.bridgeCandidateCount(), plans,
                MaterialPlanFingerprint.ofEvaluations(evaluations));
    }

    private static MaterialGenerationEvaluation stableEvaluation(MaterialBridgePlan plan) {
        MaterialGenerationRequest request = new MaterialGenerationRequest(
                plan.physicalItem(), plan.action(), plan.source(), plan.target(),
                plan.sourceMaterialId(), plan.targetMaterialId(), Optional.empty());
        MaterialGenerationEvaluation.Status status = plan.action() == MaterialBridgePlan.Action.PRESERVE
                ? MaterialGenerationEvaluation.Status.PRESERVED
                : MaterialGenerationEvaluation.Status.QUARANTINED;
        return new MaterialGenerationEvaluation(request, status,
                Optional.empty(), Optional.empty(), "determinism-test");
    }

    private static MaterialGenerationEvaluation ready(String item, String material, float durability) {
        MaterialGenerationRequest request = new MaterialGenerationRequest(
                id(item), MaterialBridgePlan.Action.BRIDGE,
                Optional.of(MaterialProfile.Ecosystem.SILENT_GEAR),
                Optional.of(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT),
                Optional.of(id(material)),
                Optional.of(GeneratedMaterialOwnership.idFor(
                        MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, id(material))),
                Optional.empty());
        TranslatedMaterialStats stats = new TranslatedMaterialStats(
                durability, 8f, 4f, 0f, id("minecraft:diamond"));
        return new MaterialGenerationEvaluation(request,
                MaterialGenerationEvaluation.Status.READY_FOR_TINKERS,
                Optional.of(stats), Optional.empty(), "test");
    }

    private static UnifiedMaterialDiscovery.Snapshot evaluatedSnapshot(
            List<MaterialGenerationEvaluation> evaluations) {
        return new UnifiedMaterialDiscovery.Snapshot(
                new MaterialCorrelationIndex(),
                new SilentGearDiscoveryBridge.DiscoveryReport(0, 0, 0),
                new TinkersCorrelationAdapter.DiscoveryReport(0, 0, 0),
                List.of(), List.of(), List.copyOf(evaluations));
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
                List.copyOf(correlated), List.copyOf(bridge), List.of());
    }

    private static MaterialProfile profile(MaterialProfile.Ecosystem ecosystem, String materialId) {
        return new MaterialProfile(ecosystem, id(materialId), List.of());
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private record Claim(String physicalItem, MaterialProfile.Ecosystem ecosystem, String materialId) {}
    private record ScanResult(int candidateCount, int correlatedCount, int bridgeCandidateCount,
                              List<MaterialBridgePlan> plans, String fingerprint) {}
}
