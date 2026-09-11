package com.anthonyahellman.silenttinkers.recipe;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearAlloyReader;
import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearStatReader;
import com.anthonyahellman.silenttinkers.material.AlloyComposition;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.SourceVisualIdentity;
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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Converts any valid Silent Gear compound alloy ingot into a tagged fluid. */
public final class SilentAlloyMeltingRecipe implements IMeltingRecipe {
    public static final int TEMPERATURE = 1200;
    private static final Set<String> LOGGED_MISSING_STAT_COMPOSITIONS = ConcurrentHashMap.newKeySet();
    private final ResourceLocation id;

    public SilentAlloyMeltingRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(IMeltingContainer inventory, Level level) {
        ItemStack sourceStack = inventory.getStack();
        Optional<AlloyComposition> composition = SilentGearAlloyReader.read(sourceStack);
        if (composition.isEmpty()) return false;
        if (SilentGearStatReader.read(sourceStack).isPresent()) return true;
        logMissingStats(composition.orElseThrow());
        return false;
    }

    @Override
    public FluidStack getOutput(IMeltingContainer inventory) {
        ItemStack sourceStack = inventory.getStack();
        Optional<AlloyComposition> composition = SilentGearAlloyReader.read(sourceStack);
        Optional<AlloyStatSnapshot> stats = SilentGearStatReader.read(sourceStack);
        if (composition.isEmpty() || stats.isEmpty()) {
            composition.ifPresent(SilentAlloyMeltingRecipe::logMissingStats);
            return FluidStack.EMPTY;
        }

        FluidStack output = new FluidStack(ModFluids.MOLTEN_COMPOSITE_ALLOY.get(), FluidValues.INGOT);
        AlloyPayload.write(output, composition.orElseThrow(),
                SilentGearAlloyReader.readStarChargeLevel(sourceStack), stats,
                SourceVisualIdentity.capture(sourceStack));
        return output;
    }

    private static void logMissingStats(AlloyComposition composition) {
        if (LOGGED_MISSING_STAT_COMPOSITIONS.add(composition.fingerprint())) {
            SilentTinkersMod.LOGGER.error(
                    "[SilentTinkers:MELTING_REFUSED_MISSING_STATS] composition={} -- alloy remains unmeltable until Silent Gear can provide evaluated stats",
                    composition.fingerprint());
        }
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
