package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class MaterialPlanFingerprintTest {
    @Test
    void fingerprintIsIndependentOfEvaluationIterationOrder() {
        MaterialGenerationEvaluation elementium = evaluation("botania", "elementium_ingot",
                MaterialGenerationEvaluation.Status.PRESERVED);
        MaterialGenerationEvaluation manasteel = evaluation("botania", "manasteel_ingot",
                MaterialGenerationEvaluation.Status.PRESERVED);

        String forward = MaterialPlanFingerprint.ofEvaluations(List.of(elementium, manasteel));
        String reverse = MaterialPlanFingerprint.ofEvaluations(List.of(manasteel, elementium));

        assertEquals(forward, reverse);
        assertEquals(16, forward.length());
    }

    @Test
    void fingerprintChangesWhenPlanStatusChanges() {
        MaterialGenerationEvaluation preserved = evaluation("botania", "elementium_ingot",
                MaterialGenerationEvaluation.Status.PRESERVED);
        MaterialGenerationEvaluation quarantined = evaluation("botania", "elementium_ingot",
                MaterialGenerationEvaluation.Status.QUARANTINED);

        assertNotEquals(
                MaterialPlanFingerprint.ofEvaluations(List.of(preserved)),
                MaterialPlanFingerprint.ofEvaluations(List.of(quarantined)));
    }

    @Test
    void fingerprintChangesWhenResolvedStatsChange() {
        MaterialGenerationEvaluation first = readyEvaluation(720.0f);
        MaterialGenerationEvaluation second = readyEvaluation(900.0f);

        assertNotEquals(
                MaterialPlanFingerprint.ofEvaluations(List.of(first)),
                MaterialPlanFingerprint.ofEvaluations(List.of(second)));
    }

    @Test
    void fingerprintIncludesDeterministicTargetIdentity() {
        MaterialGenerationEvaluation first = readyEvaluation(720.0f);
        MaterialGenerationRequest request = first.request();
        MaterialGenerationRequest changedTarget = new MaterialGenerationRequest(
                request.physicalItem(), request.action(), request.source(), request.target(),
                request.sourceMaterialId(),
                Optional.of(new ResourceLocation("silenttinkers", "generated/tinkers_construct/other/elementium")),
                request.bootstrapProfile());
        MaterialGenerationEvaluation second = new MaterialGenerationEvaluation(
                changedTarget, first.status(), first.translatedStats(), first.tinkersSourceStats(), first.detail());

        assertNotEquals(
                MaterialPlanFingerprint.ofEvaluations(List.of(first)),
                MaterialPlanFingerprint.ofEvaluations(List.of(second)));
    }

    private static MaterialGenerationEvaluation evaluation(String namespace,
                                                           String path,
                                                           MaterialGenerationEvaluation.Status status) {
        MaterialGenerationRequest request = new MaterialGenerationRequest(
                new ResourceLocation(namespace, path),
                MaterialBridgePlan.Action.PRESERVE,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return new MaterialGenerationEvaluation(
                request, status, Optional.empty(), Optional.empty(), "test");
    }

    private static MaterialGenerationEvaluation readyEvaluation(float durability) {
        ResourceLocation sourceMaterial = new ResourceLocation("silentcompat", "elementium");
        MaterialGenerationRequest request = new MaterialGenerationRequest(
                new ResourceLocation("botania", "elementium_ingot"),
                MaterialBridgePlan.Action.BRIDGE,
                Optional.of(MaterialProfile.Ecosystem.SILENT_GEAR),
                Optional.of(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT),
                Optional.of(sourceMaterial),
                Optional.of(GeneratedMaterialOwnership.idFor(
                        MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, sourceMaterial)),
                Optional.empty());
        TranslatedMaterialStats stats = new TranslatedMaterialStats(
                durability, 6.2f, 2.0f, 0.0f, new ResourceLocation("minecraft", "diamond"));
        return new MaterialGenerationEvaluation(
                request, MaterialGenerationEvaluation.Status.READY_FOR_TINKERS,
                Optional.of(stats), Optional.empty(), "test");
    }
}
