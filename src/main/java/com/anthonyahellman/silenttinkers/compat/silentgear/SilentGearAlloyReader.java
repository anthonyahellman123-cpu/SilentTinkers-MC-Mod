package com.anthonyahellman.silenttinkers.compat.silentgear;

import com.anthonyahellman.silenttinkers.material.AlloyComposition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the material shares stored on Silent Gear's generated alloy ingot.
 *
 * <p>This intentionally reads Silent Gear's stable serialized boundary instead
 * of copying its stat calculations. The resulting identity can survive melting
 * and casting even though the original {@link ItemStack} no longer exists.</p>
 */
public final class SilentGearAlloyReader {
    private static final ResourceLocation ALLOY_INGOT = new ResourceLocation("silentgear", "alloy_ingot");
    private static final String MATERIALS_KEY = "Materials";
    private static final String MATERIAL_ID_KEY = "ID";
    private static final String COUNT_KEY = "Count";
    private static final String STARCHARGED_KEY = "SG_Starcharged";

    private SilentGearAlloyReader() {
    }

    /**
     * Returns an empty value for non-alloy items, missing data, or malformed
     * material entries. No tags are created or changed while reading.
     */
    public static Optional<AlloyComposition> read(ItemStack stack) {
        if (stack.isEmpty() || !ALLOY_INGOT.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return Optional.empty();
        }

        return readMaterials(stack.getTag());
    }

    /**
     * Parses the serialized Silent Gear material list without requiring the
     * original item. Package visibility keeps this boundary directly testable.
     */
    static Optional<AlloyComposition> readMaterials(CompoundTag root) {
        if (root == null || !root.contains(MATERIALS_KEY, Tag.TAG_LIST)) {
            return Optional.empty();
        }

        Tag storedMaterials = root.get(MATERIALS_KEY);
        if (!(storedMaterials instanceof ListTag materials)) {
            return Optional.empty();
        }
        if (materials.isEmpty()) {
            return Optional.empty();
        }

        Map<ResourceLocation, Long> shares = new LinkedHashMap<>();
        try {
            if (materials.getElementType() == Tag.TAG_COMPOUND) {
                readCurrentFormat(materials, shares);
            } else if (materials.getElementType() == Tag.TAG_STRING) {
                readLegacyFormat(materials, shares);
            } else {
                return Optional.empty();
            }
            return shares.isEmpty() ? Optional.empty() : Optional.of(AlloyComposition.of(shares));
        } catch (ArithmeticException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /** Returns the Silent Gear starcharge tier, or zero when uncharged/malformed. */
    public static int readStarChargeLevel(ItemStack stack) {
        if (stack.isEmpty() || !ALLOY_INGOT.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(STARCHARGED_KEY, Tag.TAG_ANY_NUMERIC)) {
            return 0;
        }
        return Math.max(0, tag.getShort(STARCHARGED_KEY));
    }

    private static void readCurrentFormat(ListTag materials, Map<ResourceLocation, Long> shares) {
        for (Tag value : materials) {
            CompoundTag material = (CompoundTag) value;
            ResourceLocation id = ResourceLocation.tryParse(material.getString(MATERIAL_ID_KEY));
            if (id == null) {
                throw new IllegalArgumentException("Invalid Silent Gear material ID");
            }

            // Silent Gear currently writes a byte, but accepting every numeric
            // NBT width prevents add-ons from silently collapsing 70/30 to 50/50.
            long count = material.contains(COUNT_KEY, Tag.TAG_ANY_NUMERIC)
                    ? material.getLong(COUNT_KEY)
                    : 1L;
            if (count <= 0) {
                throw new IllegalArgumentException("Invalid Silent Gear material count");
            }
            shares.merge(id, count, Math::addExact);
        }
    }

    private static void readLegacyFormat(ListTag materials, Map<ResourceLocation, Long> shares) {
        for (Tag value : materials) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id == null) {
                throw new IllegalArgumentException("Invalid legacy Silent Gear material ID");
            }
            shares.merge(id, 1L, Math::addExact);
        }
    }
}
