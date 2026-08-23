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

    private static MaterialGenerationEvaluation evaluation(String namespace,
                                                           String path,
                                                           MaterialGenerationEvaluation.Status status) {
        MaterialGenerationRequest request = new MaterialGenerationRequest(
                new ResourceLocation(namespace, path),
                MaterialBridgePlan.Action.PRESERVE,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return new MaterialGenerationEvaluation(
                request, status, Optional.empty(), Optional.empty(), "test");
    }
}
