package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeadMaterialEligibilityTest {
    private static final ResourceLocation WOOD = new ResourceLocation("minecraft", "wood");

    @Test
    void zeroStatFiberCannotMasqueradeAsHeadMaterial() {
        HeadMaterialEligibility.Result result = HeadMaterialEligibility.evaluate(
                new TranslatedMaterialStats(0, 0, 0, 0, WOOD));

        assertFalse(result.eligible());
        assertEquals(HeadMaterialEligibility.Classification.REJECTED, result.classification());
    }

    @Test
    void positiveDurabilityWithoutHeadPerformanceIsStillRefused() {
        HeadMaterialEligibility.Result result = HeadMaterialEligibility.evaluate(
                new TranslatedMaterialStats(120, 0, 0, 0, WOOD));

        assertFalse(result.eligible());
        assertEquals(HeadMaterialEligibility.Classification.ROLE_LIMITED, result.classification());
    }

    @Test
    void usefulMiningOrMeleeMaterialIsEligible() {
        assertTrue(HeadMaterialEligibility.evaluate(
                new TranslatedMaterialStats(120, 4, 0, 0, WOOD)).eligible());
        assertTrue(HeadMaterialEligibility.evaluate(
                new TranslatedMaterialStats(120, 0, 2, 0, WOOD)).eligible());
    }
}
