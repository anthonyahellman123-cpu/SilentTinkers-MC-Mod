package com.anthonyahellman.silenttinkers.compat.silentgear;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.api.material.IMaterial;
import net.silentchaos512.gear.api.material.IMaterialInstance;
import net.silentchaos512.gear.api.part.PartType;
import net.silentchaos512.gear.gear.material.MaterialManager;
import net.silentchaos512.gear.gear.material.MaterialInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Native Silent Gear 3.6.7 catalog provider. */
public final class SilentGearApiProvider implements SilentGearDiscoveryBridge.Provider {
    private static final Set<ResourceLocation> LOGGED_TRAIT_READ_FAILURES = ConcurrentHashMap.newKeySet();

    @Override
    public Collection<SilentGearDiscoveryBridge.Entry> discover() {
        List<SilentGearDiscoveryBridge.Entry> result = new ArrayList<>();

        for (IMaterial material : MaterialManager.getValues()) {
            ResourceLocation materialId = material.getId();
            Set<ResourceLocation> physicalItems = new LinkedHashSet<>();
            Set<ResourceLocation> traitIds = new LinkedHashSet<>();

            for (ItemStack stack : material.getIngredient().getItems()) {
                if (!stack.isEmpty()) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId != null) {
                        physicalItems.add(itemId);
                    }
                    try {
                        IMaterialInstance instance = MaterialInstance.of(material, stack);
                        instance.getTraits(PartType.MAIN, GearType.ALL, ItemStack.EMPTY).stream()
                                .map(trait -> trait.getTraitId())
                                .forEach(traitIds::add);
                    } catch (RuntimeException | LinkageError exception) {
                        if (LOGGED_TRAIT_READ_FAILURES.add(materialId)) {
                            SilentTinkersMod.LOGGER.warn(
                                    "[SilentTinkers:SG_TRAIT_READ_FAILED] material={} item={} -- material discovery preserved without trait adapters",
                                    materialId, itemId, exception);
                        }
                    }
                }
            }

            result.add(new SilentGearDiscoveryBridge.Entry(
                    materialId,
                    Set.copyOf(physicalItems),
                    List.copyOf(traitIds)));
        }

        return List.copyOf(result);
    }
}
