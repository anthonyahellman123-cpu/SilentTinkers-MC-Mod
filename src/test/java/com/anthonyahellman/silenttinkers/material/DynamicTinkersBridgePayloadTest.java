package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicTinkersBridgePayloadTest {
    @Test
    void readySilentGearEvaluationBecomesSingleMaterialPayload() {
        ResourceLocation item = id("example:mythril_ingot");
        ResourceLocation material = id("silentcompat:mythril");
        TranslatedMaterialStats translated = new TranslatedMaterialStats(
                900f, 12f, 5.5f, 0.15f, id("minecraft:diamond"));
        MaterialGenerationRequest request = new MaterialGenerationRequest(
                item,
                MaterialBridgePlan.Action.BRIDGE,
                Optional.of(MaterialProfile.Ecosystem.SILENT_GEAR),
                Optional.of(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT),
                Optional.of(material),
                Optional.empty());
        MaterialGenerationEvaluation evaluation = new MaterialGenerationEvaluation(
                request,
                MaterialGenerationEvaluation.Status.READY_FOR_TINKERS,
                Optional.of(translated),
                Optional.empty(),
                "test");

        DynamicTinkersBridgePayload payload = DynamicTinkersBridgePayload.from(evaluation).orElseThrow();

        assertEquals(1, payload.composition().ingredients().size());
        assertEquals(material, payload.composition().ingredients().get(0).materialId());
        assertEquals(1L, payload.composition().ingredients().get(0).units());
        assertEquals(translated.durability(), payload.stats().durability());
        assertEquals(translated.miningSpeed(), payload.stats().miningSpeed());
        assertEquals(translated.meleeDamage(), payload.stats().meleeDamage());
        assertEquals(translated.attackSpeed(), payload.stats().attackSpeed());
        assertEquals(translated.harvestTier(), payload.stats().harvestTier());
    }

    @Test
    void nonReadyEvaluationCannotCreateMutationPayload() {
        MaterialGenerationRequest request = new MaterialGenerationRequest(
                id("example:bronze_ingot"),
                MaterialBridgePlan.Action.PRESERVE,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        MaterialGenerationEvaluation evaluation = new MaterialGenerationEvaluation(
                request,
                MaterialGenerationEvaluation.Status.PRESERVED,
                Optional.empty(), Optional.empty(), "native");

        assertTrue(DynamicTinkersBridgePayload.from(evaluation).isEmpty());
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
