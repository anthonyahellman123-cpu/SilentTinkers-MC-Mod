package com.anthonyahellman.silenttinkers.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Optional;

/**
 * Versioned envelope used on every item and fluid that carries a dynamic alloy.
 * Keeping one envelope prevents the smeltery, casts, and future tool parts from
 * slowly developing incompatible NBT formats.
 */
public final class AlloyPayload {
    public static final String ROOT_KEY = "SilentTinkersAlloy";

    private AlloyPayload() {}

    public static void write(ItemStack stack, AlloyComposition composition) {
        stack.getOrCreateTag().put(ROOT_KEY, composition.save());
    }

    public static void write(FluidStack stack, AlloyComposition composition) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(ROOT_KEY, composition.save());
    }

    public static Optional<AlloyComposition> read(ItemStack stack) {
        return read(stack.getTag());
    }

    public static Optional<AlloyComposition> read(FluidStack stack) {
        return read(stack.getTag());
    }

    public static Optional<AlloyComposition> read(CompoundTag carrierTag) {
        if (carrierTag == null || !carrierTag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        try {
            return Optional.of(AlloyComposition.load(carrierTag.getCompound(ROOT_KEY)));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.empty();
        }
    }
}
