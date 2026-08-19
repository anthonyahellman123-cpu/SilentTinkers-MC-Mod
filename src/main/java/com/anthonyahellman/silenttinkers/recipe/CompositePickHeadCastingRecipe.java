package com.anthonyahellman.silenttinkers.recipe;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.AbstractCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.tools.TinkerToolParts;

/** First real Tinkers part produced from a dynamic Silent Gear alloy. */
public final class CompositePickHeadCastingRecipe extends AbstractCastingRecipe {
    public static final MaterialId MATERIAL = new MaterialId(SilentTinkersMod.MOD_ID, "composite_alloy");
    private static final int COST = 2 * FluidValues.INGOT;
    private static final TagKey<Item> PICK_HEAD_CASTS = TagKey.create(
            Registries.ITEM, new ResourceLocation("tconstruct", "casts/multi_use/pick_head"));

    public CompositePickHeadCastingRecipe(ResourceLocation id) {
        super(TinkerRecipeTypes.CASTING_TABLE.get(), id, "silent_gear_alloys",
                Ingredient.of(PICK_HEAD_CASTS), false, false);
    }

    @Override
    public boolean matches(ICastingContainer inventory, Level level) {
        return getCast().test(inventory.getStack())
                && inventory.getFluid() == ModFluids.MOLTEN_COMPOSITE_ALLOY.get()
                && AlloyPayload.read(inventory.getFluidTag()).isPresent();
    }

    @Override
    public int getFluidAmount(ICastingContainer inventory) {
        return COST;
    }

    @Override
    public int getCoolingTime(ICastingContainer inventory) {
        return 100;
    }

    @Override
    public ItemStack assemble(ICastingContainer inventory, RegistryAccess access) {
        return AlloyPayload.read(inventory.getFluidTag()).map(composition -> {
            int starChargeLevel = AlloyPayload.readStarChargeLevel(inventory.getFluidTag());
            MaterialVariantId variant = MaterialVariantId.create(
                    MATERIAL, AlloyVariantCodec.encode(
                            composition, starChargeLevel, AlloyPayload.readStats(inventory.getFluidTag())));
            ItemStack part = TinkerToolParts.pickHead.get().withMaterial(variant);
            AlloyPayload.write(part, composition, starChargeLevel, AlloyPayload.readStats(inventory.getFluidTag()));
            return part;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return TinkerToolParts.pickHead.get().withMaterialForDisplay(MATERIAL);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPOSITE_PICK_HEAD_CASTING.get();
    }
}
