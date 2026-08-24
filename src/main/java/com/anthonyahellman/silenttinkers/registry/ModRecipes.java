package com.anthonyahellman.silenttinkers.registry;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.recipe.CompositeAlloyCastingRecipe;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
import com.anthonyahellman.silenttinkers.recipe.IdOnlyRecipeSerializer;
import com.anthonyahellman.silenttinkers.recipe.SilentAlloyMeltingRecipe;
import com.anthonyahellman.silenttinkers.recipe.SilentMaterialMeltingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SilentTinkersMod.MOD_ID);

    public static final RegistryObject<RecipeSerializer<SilentAlloyMeltingRecipe>> SILENT_ALLOY_MELTING =
            SERIALIZERS.register("silent_alloy_melting",
                    () -> new IdOnlyRecipeSerializer<>(SilentAlloyMeltingRecipe::new));
    public static final RegistryObject<RecipeSerializer<SilentMaterialMeltingRecipe>> SILENT_MATERIAL_MELTING =
            SERIALIZERS.register("silent_material_melting",
                    () -> new IdOnlyRecipeSerializer<>(SilentMaterialMeltingRecipe::new));
    public static final RegistryObject<RecipeSerializer<CompositeAlloyCastingRecipe>> COMPOSITE_ALLOY_CASTING =
            SERIALIZERS.register("composite_alloy_casting",
                    () -> new IdOnlyRecipeSerializer<>(CompositeAlloyCastingRecipe::new));
    public static final RegistryObject<RecipeSerializer<CompositePickHeadCastingRecipe>> COMPOSITE_PICK_HEAD_CASTING =
            SERIALIZERS.register("composite_pick_head_casting",
                    () -> new IdOnlyRecipeSerializer<>(CompositePickHeadCastingRecipe::new));

    private ModRecipes() {}
}
