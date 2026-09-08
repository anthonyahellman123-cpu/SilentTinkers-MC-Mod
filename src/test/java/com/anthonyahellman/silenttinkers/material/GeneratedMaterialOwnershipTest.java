package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeneratedMaterialOwnershipTest {
    @Test
    void generatedIdIsDeterministicAndSelfDescribing() {
        ResourceLocation source = id("tinkers_advanced:neutronium");

        ResourceLocation first = GeneratedMaterialOwnership.idFor(
                MaterialProfile.Ecosystem.SILENT_GEAR, source);
        ResourceLocation second = GeneratedMaterialOwnership.idFor(
                MaterialProfile.Ecosystem.SILENT_GEAR, source);
        GeneratedMaterialOwnership.Ownership ownership =
                GeneratedMaterialOwnership.describe(first).orElseThrow();

        assertEquals(first, second);
        assertEquals(id("silenttinkers:generated/silent_gear/tinkers_advanced/neutronium"), first);
        assertEquals(MaterialProfile.Ecosystem.SILENT_GEAR, ownership.target());
        assertEquals(source, ownership.sourceMaterialId());
    }

    @Test
    void foreignAndMalformedIdsAreNotClaimed() {
        assertFalse(GeneratedMaterialOwnership.isOwned(id("silentgear:generated/example/bronze")));
        assertFalse(GeneratedMaterialOwnership.isOwned(id("silenttinkers:generated/not_an_ecosystem/example/bronze")));
        assertFalse(GeneratedMaterialOwnership.isOwned(id("silenttinkers:generated/silent_gear")));
        assertTrue(GeneratedMaterialOwnership.isOwned(
                GeneratedMaterialOwnership.idFor(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT,
                        id("silentcompat:elementium"))));
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
