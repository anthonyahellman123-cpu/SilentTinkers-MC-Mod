package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable mutation payload consumed by the dynamic tagged-fluid Tinkers path.
 * Building this payload is the final validation step before a runtime recipe is
 * allowed to create bridge fluid or tool parts.
 */
public record DynamicTinkersBridgePayload(
        AlloyComposition composition,
        AlloyStatSnapshot stats) {

    public static Optional<DynamicTinkersBridgePayload> from(MaterialGenerationEvaluation evaluation) {
        if (!evaluation.readyForMutation()) return Optional.empty();

        ResourceLocation sourceMaterial = evaluation.request().sourceMaterialId().orElse(null);
        TranslatedMaterialStats translated = evaluation.translatedStats().orElse(null);
        if (sourceMaterial == null || translated == null) return Optional.empty();

        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        ingredients.put(sourceMaterial, 1L);
        AlloyComposition composition = AlloyComposition.of(ingredients);
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                translated.durability(),
                translated.miningSpeed(),
                translated.meleeDamage(),
                translated.attackSpeed(),
                translated.harvestTier());
        return Optional.of(new DynamicTinkersBridgePayload(composition, stats));
    }
}
