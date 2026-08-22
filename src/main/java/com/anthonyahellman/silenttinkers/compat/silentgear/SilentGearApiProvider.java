package com.anthonyahellman.silenttinkers.compat.silentgear;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.silentchaos512.gear.api.material.Material;
import net.silentchaos512.gear.setup.SgRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Native Silent Gear 3.6.x catalog provider.
 *
 * <p>Silent Gear's own material book enumerates {@link SgRegistries#MATERIAL},
 * and each material exposes its crafting {@code Ingredient}. We use those two
 * authoritative surfaces to discover material IDs and the concrete item aliases
 * accepted by the material definition. No crafted gear NBT is required.</p>
 */
public final class SilentGearApiProvider implements SilentGearDiscoveryBridge.Provider {
    @Override
    public Collection<SilentGearDiscoveryBridge.Entry> discover() {
        List<SilentGearDiscoveryBridge.Entry> result = new ArrayList<>();

        for (Material material : SgRegistries.MATERIAL) {
            ResourceLocation materialId = SgRegistries.MATERIAL.getKey(material);
            if (materialId == null || !material.isValid()) {
                continue;
            }

            Set<ResourceLocation> physicalItems = new LinkedHashSet<>();
            for (ItemStack stack : material.getIngredient().getItems()) {
                if (!stack.isEmpty()) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId != null) {
                        physicalItems.add(itemId);
                    }
                }
            }

            // Trait extraction is intentionally a later pass. Material traits in
            // Silent Gear are context-sensitive (part/gear/property dependent),
            // while physical catalog correlation only needs ID + ingredient aliases.
            result.add(new SilentGearDiscoveryBridge.Entry(
                    materialId,
                    Set.copyOf(physicalItems),
                    List.of()));
        }

        return List.copyOf(result);
    }
}
