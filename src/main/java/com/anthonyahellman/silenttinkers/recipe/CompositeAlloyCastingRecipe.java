package com.anthonyahellman.silenttinkers.recipe;

import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModItems;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.AbstractCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;

/** Casts the diagnostic sample while copying the fluid's alloy payload. */
public final class CompositeAlloyCastingRecipe extends AbstractCastingRecipe {
    public CompositeAlloyCastingRecipe(ResourceLocation id) {
        super(TinkerRecipeTypes.CASTING_TABLE.get(), id, "silent_gear_alloys", Ingredient.EMPTY, false, false);
    }

    @Override
    public boolean matches(ICastingContainer inventory, Level level) {
        return inventory.getStack().isEmpty()
                && inventory.getFluid() == ModFluids.MOLTEN_COMPOSITE_ALLOY.get()
                && AlloyPayload.read(inventory.getFluidTag()).isPresent();
    }

    @Override
    public int getFluidAmount(ICastingContainer inventory) {
        return FluidValues.INGOT;
    }

    @Override
    public int getCoolingTime(ICastingContainer inventory) {
        return 60;
    }

    @Override
    public ItemStack assemble(ICastingContainer inventory, RegistryAccess access) {
        ItemStack result = new ItemStack(ModItems.COMPOSITE_ALLOY_SAMPLE.get());
        AlloyPayload.read(inventory.getFluidTag()).ifPresent(composition -> AlloyPayload.write(
                result, composition, AlloyPayload.readStarChargeLevel(inventory.getFluidTag()),
                AlloyPayload.readStats(inventory.getFluidTag()),
                AlloyPayload.readVisualSource(inventory.getFluidTag())));
        return result;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return new ItemStack(ModItems.COMPOSITE_ALLOY_SAMPLE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPOSITE_ALLOY_CASTING.get();
    }
}
