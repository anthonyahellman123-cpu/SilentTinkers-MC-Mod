package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AlloyDisplayFormatterTest {
    @Test
    void namesPureMaterialWithoutCompositePlaceholder() {
        AlloyComposition composition = AlloyComposition.of(Map.of(id("silentcompat:elementium"), 1L));

        assertEquals("Elementium", AlloyDisplayFormatter.compositionLabel(composition));
        assertEquals("Molten Elementium (100%)", AlloyDisplayFormatter.moltenLabel(composition));
    }

    @Test
    void namesMixedAlloyWithExactPercentagesEverywhere() {
        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        ingredients.put(id("silentgear:iron"), 30L);
        ingredients.put(id("silentgear:redstone"), 70L);
        AlloyComposition composition = AlloyComposition.of(ingredients);

        assertEquals("Alloy: Iron 30% + Redstone 70%", AlloyDisplayFormatter.compositionLabel(composition));
        assertEquals("Molten Alloy: Iron 30% + Redstone 70%", AlloyDisplayFormatter.moltenLabel(composition));
    }

    @Test
    void keepsUsefulPrecisionForThreeWayAlloys() {
        AlloyComposition composition = AlloyComposition.of(Map.of(
                id("silentgear:iron"), 1L,
                id("silentgear:redstone"), 1L,
                id("silentgear:diamond"), 1L));

        assertEquals("Alloy: Diamond 33.3% + Iron 33.3% + Redstone 33.3%",
                AlloyDisplayFormatter.compositionLabel(composition));
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
