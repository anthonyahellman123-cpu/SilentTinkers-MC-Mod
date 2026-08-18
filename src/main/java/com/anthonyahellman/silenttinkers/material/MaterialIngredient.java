package com.anthonyahellman.silenttinkers.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** One positive, integral share of a material in a normalized alloy recipe. */
public record MaterialIngredient(ResourceLocation materialId, long units) {
    private static final String MATERIAL_KEY = "Material";
    private static final String UNITS_KEY = "Units";

    public MaterialIngredient {
        Objects.requireNonNull(materialId, "materialId");
        if (units <= 0) {
            throw new IllegalArgumentException("Material units must be positive");
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(MATERIAL_KEY, materialId.toString());
        tag.putLong(UNITS_KEY, units);
        return tag;
    }

    public static MaterialIngredient load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(MATERIAL_KEY));
        if (id == null) {
            throw new IllegalArgumentException("Invalid material ID in alloy data");
        }
        return new MaterialIngredient(id, tag.getLong(UNITS_KEY));
    }
}
