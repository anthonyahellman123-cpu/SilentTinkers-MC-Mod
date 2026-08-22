package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearStatReader;
import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersStatReader;
import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersStatSnapshot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

/**
 * Resolves generation requests using the source ecosystem as the authority.
 * Cross-ecosystem conversions that are not yet semantically defined fail closed
 * instead of silently inventing values.
 */
public final class MaterialStatTranslator {
    private MaterialStatTranslator() {}

    public static Optional<TranslatedMaterialStats> translate(MaterialGenerationRequest request) {
        if (request.action() != MaterialBridgePlan.Action.BRIDGE) return Optional.empty();

        MaterialProfile.Ecosystem source = request.source().orElse(null);
        if (source == null) return Optional.empty();

        return switch (source) {
            case SILENT_GEAR -> translateSilentGear(request);
            case TINKERS_CONSTRUCT -> Optional.empty();
        };
    }

    public static Optional<TinkersStatSnapshot> readNativeTinkers(MaterialGenerationRequest request) {
        if (request.source().orElse(null) != MaterialProfile.Ecosystem.TINKERS_CONSTRUCT) {
            return Optional.empty();
        }
        return request.sourceMaterialId().flatMap(TinkersStatReader::read);
    }

    private static Optional<TranslatedMaterialStats> translateSilentGear(MaterialGenerationRequest request) {
        Item item = ForgeRegistries.ITEMS.getValue(request.physicalItem());
        if (item == null) return Optional.empty();

        return SilentGearStatReader.read(new ItemStack(item))
                .map(TranslatedMaterialStats::fromSilentGear)
                .map(MaterialTranslationPolicy::apply);
    }
}
