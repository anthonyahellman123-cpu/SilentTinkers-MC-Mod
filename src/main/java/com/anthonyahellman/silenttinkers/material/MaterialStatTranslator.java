package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearStatReader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

/**
 * Resolves a generation request into ecosystem-neutral stats using the source
 * ecosystem as the authority. Unsupported directions fail closed so generation
 * can quarantine them instead of inventing values.
 */
public final class MaterialStatTranslator {
    private MaterialStatTranslator() {}

    public static Optional<TranslatedMaterialStats> translate(MaterialGenerationRequest request) {
        if (request.action() == MaterialBridgePlan.Action.PRESERVE) {
            return Optional.empty();
        }

        if (request.action() == MaterialBridgePlan.Action.BOOTSTRAP) {
            // Bootstrap profiles currently contain relative multipliers rather
            // than absolute tool stats. Their concrete conversion belongs in the
            // target generator, not here.
            return Optional.empty();
        }

        MaterialProfile.Ecosystem source = request.source().orElse(null);
        if (source == null) {
            return Optional.empty();
        }

        return switch (source) {
            case SILENT_GEAR -> translateSilentGear(request);
            case TINKERS_CONSTRUCT -> Optional.empty();
        };
    }

    private static Optional<TranslatedMaterialStats> translateSilentGear(MaterialGenerationRequest request) {
        Item item = ForgeRegistries.ITEMS.getValue(request.physicalItem());
        if (item == null) {
            return Optional.empty();
        }

        return SilentGearStatReader.read(new ItemStack(item))
                .map(TranslatedMaterialStats::fromSilentGear);
    }
}
