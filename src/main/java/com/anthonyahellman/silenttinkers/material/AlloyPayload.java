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
    private static final String STAR_CHARGE_KEY = "StarChargeLevel";
    private static final String STATS_KEY = "EvaluatedStats";
    private static final String VISUAL_SOURCE_KEY = "VisualSource";

    private AlloyPayload() {}

    public static void write(ItemStack stack, AlloyComposition composition) {
        write(stack, composition, 0);
    }

    public static void write(FluidStack stack, AlloyComposition composition) {
        write(stack, composition, 0);
    }

    public static void write(ItemStack stack, AlloyComposition composition, int starChargeLevel) {
        write(stack, composition, starChargeLevel, Optional.empty());
    }

    public static void write(FluidStack stack, AlloyComposition composition, int starChargeLevel) {
        write(stack, composition, starChargeLevel, Optional.empty());
    }

    public static void write(ItemStack stack, AlloyComposition composition, int starChargeLevel,
                             Optional<AlloyStatSnapshot> stats) {
        write(stack, composition, starChargeLevel, stats, Optional.empty());
    }

    public static void write(FluidStack stack, AlloyComposition composition, int starChargeLevel,
                             Optional<AlloyStatSnapshot> stats) {
        write(stack, composition, starChargeLevel, stats, Optional.empty());
    }

    public static void write(ItemStack stack, AlloyComposition composition, int starChargeLevel,
                             Optional<AlloyStatSnapshot> stats,
                             Optional<SourceVisualIdentity> visualSource) {
        stack.getOrCreateTag().put(ROOT_KEY, createRoot(composition, starChargeLevel, stats, visualSource));
    }

    public static void write(FluidStack stack, AlloyComposition composition, int starChargeLevel,
                             Optional<AlloyStatSnapshot> stats,
                             Optional<SourceVisualIdentity> visualSource) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(ROOT_KEY, createRoot(composition, starChargeLevel, stats, visualSource));
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

    public static int readStarChargeLevel(ItemStack stack) {
        return readStarChargeLevel(stack.getTag());
    }

    public static int readStarChargeLevel(FluidStack stack) {
        return readStarChargeLevel(stack.getTag());
    }

    public static int readStarChargeLevel(CompoundTag carrierTag) {
        if (carrierTag == null || !carrierTag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag root = carrierTag.getCompound(ROOT_KEY);
        return root.contains(STAR_CHARGE_KEY, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0, root.getInt(STAR_CHARGE_KEY))
                : 0;
    }

    public static Optional<AlloyStatSnapshot> readStats(ItemStack stack) {
        return readStats(stack.getTag());
    }

    public static Optional<AlloyStatSnapshot> readStats(FluidStack stack) {
        return readStats(stack.getTag());
    }

    public static Optional<AlloyStatSnapshot> readStats(CompoundTag carrierTag) {
        if (carrierTag == null || !carrierTag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag root = carrierTag.getCompound(ROOT_KEY);
        return root.contains(STATS_KEY, Tag.TAG_COMPOUND)
                ? AlloyStatSnapshot.load(root.getCompound(STATS_KEY))
                : Optional.empty();
    }

    public static Optional<SourceVisualIdentity> readVisualSource(ItemStack stack) {
        return readVisualSource(stack.getTag());
    }

    public static Optional<SourceVisualIdentity> readVisualSource(FluidStack stack) {
        return readVisualSource(stack.getTag());
    }

    public static Optional<SourceVisualIdentity> readVisualSource(CompoundTag carrierTag) {
        if (carrierTag == null || !carrierTag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag root = carrierTag.getCompound(ROOT_KEY);
        return root.contains(VISUAL_SOURCE_KEY, Tag.TAG_COMPOUND)
                ? SourceVisualIdentity.load(root.getCompound(VISUAL_SOURCE_KEY))
                : Optional.empty();
    }

    private static CompoundTag createRoot(AlloyComposition composition, int starChargeLevel,
                                          Optional<AlloyStatSnapshot> stats,
                                          Optional<SourceVisualIdentity> visualSource) {
        CompoundTag root = composition.save();
        if (starChargeLevel > 0) {
            root.putInt(STAR_CHARGE_KEY, starChargeLevel);
        }
        stats.ifPresent(value -> root.put(STATS_KEY, value.save()));
        visualSource.ifPresent(value -> root.put(VISUAL_SOURCE_KEY, value.save()));
        return root;
    }
}
