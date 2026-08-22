package com.anthonyahellman.silenttinkers.compat.tconstruct;

import com.anthonyahellman.silenttinkers.material.MaterialProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only discovery adapter for Tinkers Construct's loaded material registry.
 *
 * <p>This class deliberately does not create materials or mutate Tinkers data.
 * It asks Tinkers for its visible materials, variants and material recipes, then
 * reports the physical item candidates Tinkers itself associates with them.</p>
 */
public final class TinkersMaterialDiscovery {
    private TinkersMaterialDiscovery() {}

    public static List<DiscoveredMaterial> discover() {
        if (!MaterialRegistry.isFullyLoaded()) {
            return List.of();
        }

        List<DiscoveredMaterial> result = new ArrayList<>();
        for (IMaterial material : MaterialRegistry.getMaterials()) {
            MaterialId materialId = material.getIdentifier();
            MaterialProfile profile = new MaterialProfile(
                    MaterialProfile.Ecosystem.TINKERS_CONSTRUCT,
                    materialId.getId(),
                    Set.of());

            Set<ResourceLocation> physicalItems = new LinkedHashSet<>();
            Collection<MaterialVariantId> variants = MaterialRecipeCache.getVariants(materialId);
            for (MaterialVariantId variant : variants) {
                for (ItemStack stack : MaterialRecipeCache.getItems(variant)) {
                    if (!stack.isEmpty()) {
                        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                        if (itemId != null) {
                            physicalItems.add(itemId);
                        }
                    }
                }
            }

            result.add(new DiscoveredMaterial(profile, Set.copyOf(physicalItems)));
        }
        return List.copyOf(result);
    }

    /**
     * A Tinkers profile plus the concrete physical items accepted by its loaded
     * material recipes. Empty physicalItems means the material is known to
     * Tinkers but has no discoverable item recipe (e.g. an uncraftable material).
     */
    public record DiscoveredMaterial(
            MaterialProfile profile,
            Set<ResourceLocation> physicalItems) {}
}
