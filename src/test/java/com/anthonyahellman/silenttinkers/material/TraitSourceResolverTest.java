package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TraitSourceResolverTest {
    @Test
    void resolutionUsesExactThenTconstructThenUniquePath() {
        List<ResourceLocation> ids = List.of(
                id("tinkers_advanced:neutronium"),
                id("tconstruct:iron"),
                id("addon:elementium"));

        assertResolution("tinkers_advanced:neutronium", ids,
                TraitSourceResolver.Status.EXACT, "tinkers_advanced:neutronium");
        assertResolution("silentgear:iron", ids,
                TraitSourceResolver.Status.TCONSTRUCT_PATH, "tconstruct:iron");
        assertResolution("silentcompat:elementium", ids,
                TraitSourceResolver.Status.UNIQUE_PATH, "addon:elementium");
    }

    @Test
    void ambiguousSamePathIsSortedAndRefused() {
        TraitSourceResolver.Resolution resolution = TraitSourceResolver.resolve(
                id("silentcompat:paracausal_alloy"),
                List.of(id("zeta:paracausal_alloy"), id("alpha:paracausal_alloy")));

        assertEquals(TraitSourceResolver.Status.AMBIGUOUS, resolution.status());
        assertFalse(resolution.resolved());
        assertTrue(resolution.materialId().isEmpty());
        assertEquals(List.of(id("alpha:paracausal_alloy"), id("zeta:paracausal_alloy")),
                resolution.candidates());
    }

    @Test
    void duplicateClaimsDoNotCreateFalseAmbiguity() {
        TraitSourceResolver.Resolution resolution = TraitSourceResolver.resolve(
                id("silentcompat:elementium"),
                List.of(id("botania:elementium"), id("botania:elementium")));

        assertEquals(TraitSourceResolver.Status.UNIQUE_PATH, resolution.status());
        assertEquals(id("botania:elementium"), resolution.materialId().orElseThrow());
    }

    @Test
    void absentPathIsUnresolved() {
        TraitSourceResolver.Resolution resolution = TraitSourceResolver.resolve(
                id("silentcompat:destiny_unknown"), List.of(id("tconstruct:iron")));

        assertEquals(TraitSourceResolver.Status.UNRESOLVED, resolution.status());
        assertFalse(resolution.resolved());
        assertTrue(resolution.candidates().isEmpty());
    }

    private static void assertResolution(String source, List<ResourceLocation> ids,
                                         TraitSourceResolver.Status status, String expected) {
        TraitSourceResolver.Resolution resolution = TraitSourceResolver.resolve(id(source), ids);
        assertEquals(status, resolution.status());
        assertEquals(id(expected), resolution.materialId().orElseThrow());
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
