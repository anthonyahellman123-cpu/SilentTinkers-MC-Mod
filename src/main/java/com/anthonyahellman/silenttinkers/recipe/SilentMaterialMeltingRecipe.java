package com.anthonyahellman.silenttinkers.recipe;

import com.anthonyahellman.silenttinkers.material.AlloyComposition;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.MaterialDiscoveryState;
import com.anthonyahellman.silenttinkers.material.MaterialGenerationEvaluation;
import com.anthonyahellman.silenttinkers.material.TranslatedMaterialStats;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * First mutation consumer of the unified bridge planner.
 *
 * <p>Any canonical Silent Gear material that completed discovery with
 * READY_FOR_TINKERS can be melted into SilentTinkers' tagged composite fluid.
 * The fluid carries a 100% single-material composition plus the already
 * evaluated/translated stats, allowing the existing composite casting path to
 * create a real Tinkers material variant without hot-registering datapack data
 * after Tinkers has loaded.</p>
 */
public final class SilentMaterialMeltingRecipe implements IMeltingRecipe {
    public static final int TEMPERATURE = 1200;
    private final ResourceLocation id;

    public SilentMaterialMeltingRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(IMeltingContainer inventory, Level level) {
        return findReadyEvaluation(inventory.getStack()).isPresent();
    }

    @Override
    public FluidStack getOutput(IMeltingContainer inventory) {
        Optional<MaterialGenerationEvaluation> evaluation = findReadyEvaluation(inventory.getStack());
        if (evaluation.isEmpty()) return FluidStack.EMPTY;

        MaterialGenerationEvaluation ready = evaluation.get();
        ResourceLocation sourceMaterial = ready.request().sourceMaterialId().orElse(null);
        TranslatedMaterialStats translated = ready.translatedStats().orElse(null);
        if (sourceMaterial == null || translated == null) return FluidStack.EMPTY;

        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        ingredients.put(sourceMaterial, 1L);
        AlloyComposition composition = AlloyComposition.of(ingredients);
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                translated.durability(), translated.miningSpeed(), translated.meleeDamage(),
                translated.attackSpeed(), translated.harvestTier());

        FluidStack output = new FluidStack(ModFluids.MOLTEN_COMPOSITE_ALLOY.get(), FluidValues.INGOT);
        AlloyPayload.write(output, composition, 0, Optional.of(stats));
        return output;
    }

    private static Optional<MaterialGenerationEvaluation> findReadyEvaluation(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return Optional.empty();
        return MaterialDiscoveryState.readyForTinkers(itemId);
    }

    @Override
    public int getTemperature(IMeltingContainer inventory) {
        return TEMPERATURE;
    }

    @Override
    public int getTime(IMeltingContainer inventory) {
        return IMeltingRecipe.calcTimeForAmount(TEMPERATURE, FluidValues.INGOT);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String getGroup() {
        return "silent_material_bridges";
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess access) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SILENT_MATERIAL_MELTING.get();
    }
}
