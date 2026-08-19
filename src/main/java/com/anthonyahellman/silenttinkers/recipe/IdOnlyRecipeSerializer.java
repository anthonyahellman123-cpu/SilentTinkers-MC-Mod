package com.anthonyahellman.silenttinkers.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;
import java.util.function.Function;

/** Serializer for recipes whose behavior is entirely defined in code. */
public final class IdOnlyRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
    private final Function<ResourceLocation, T> factory;

    public IdOnlyRecipeSerializer(Function<ResourceLocation, T> factory) {
        this.factory = factory;
    }

    @Override
    public T fromJson(ResourceLocation id, JsonObject json) {
        return factory.apply(id);
    }

    @Override
    @Nullable
    public T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        return factory.apply(id);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, T recipe) {}
}
