package com.anthonyahellman.silenttinkers.compat.silentgear;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.silentchaos512.gear.api.material.IMaterial;
import net.silentchaos512.gear.gear.material.MaterialManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Native Silent Gear 3.6.7 catalog provider. */
public final class SilentGearApiProvider implements SilentGearDiscoveryBridge.Provider {
    @Override
    public Collection<SilentGearDiscoveryBridge.Entry> discover() {
        List<SilentGearDiscoveryBridge.Entry> result = new ArrayList<>();

        for (IMaterial material : MaterialManager.getValues()) {
            ResourceLocation materialId = material.getId();
            Set<ResourceLocation> physicalItems = new LinkedHashSet<>();

            for (ItemStack stack : material.getIngredient().getItems()) {
                if (!stack.isEmpty()) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId != null) {
                        physicalItems.add(itemId);
                    }
                }
            }

            // SG traits depend on material instance/part/gear context. Keep this
            // first catalog pass focused on identity + physical crafting aliases.
            result.add(new SilentGearDiscoveryBridge.Entry(
                    materialId,
                    Set.copyOf(physicalItems),
                    List.of()));
        }

        return List.copyOf(result);
    }
}
