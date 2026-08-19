package com.anthonyahellman.silenttinkers.recipe;

import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearAlloyReader;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;

/** Converts any valid Silent Gear compound alloy ingot into a tagged fluid. */
public final class SilentAlloyMeltingRecipe implements IMeltingRecipe {
    public static final int TEMPERATURE = 1200;
    private final ResourceLocation id;

    public SilentAlloyMeltingRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(IMeltingContainer inventory, Level level) {
        return SilentGearAlloyReader.read(inventory.getStack()).isPresent();
    }

    @Override
    public FluidStack getOutput(IMeltingContainer inventory) {
        FluidStack output = new FluidStack(ModFluids.MOLTEN_COMPOSITE_ALLOY.get(), FluidValues.INGOT);
        SilentGearAlloyReader.read(inventory.getStack())
                .ifPresent(composition -> AlloyPayload.write(output, composition));
        return output;
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
        return "silent_gear_alloys";
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
        return ModRecipes.SILENT_ALLOY_MELTING.get();
    }
}
