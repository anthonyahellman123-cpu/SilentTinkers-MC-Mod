package com.anthonyahellman.silenttinkers.compat.silentgear;

import com.anthonyahellman.silenttinkers.material.AlloyComposition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SilentGearAlloyReaderTest {
    @Test
    void preservesThirtySeventyWhenCountsUseSilentGearsByteFormat() {
        CompoundTag root = alloy(material("silentgear:iron", 30, NumericWidth.BYTE),
                material("silentgear:redstone", 70, NumericWidth.BYTE));

        AlloyComposition composition = SilentGearAlloyReader.readMaterials(root).orElseThrow();

        assertEquals(0.30, composition.fraction(id("silentgear:iron")), 0.000_001);
        assertEquals(0.70, composition.fraction(id("silentgear:redstone")), 0.000_001);
    }

    @Test
    void acceptsAddonCountsStoredAsAnyNumericNbtWidth() {
        CompoundTag root = alloy(material("silentgear:iron", 30, NumericWidth.SHORT),
                material("silentgear:redstone", 70, NumericWidth.INT),
                material("silentgear:diamond", 100, NumericWidth.LONG));

        AlloyComposition composition = SilentGearAlloyReader.readMaterials(root).orElseThrow();

        assertEquals(0.15, composition.fraction(id("silentgear:iron")), 0.000_001);
        assertEquals(0.35, composition.fraction(id("silentgear:redstone")), 0.000_001);
        assertEquals(0.50, composition.fraction(id("silentgear:diamond")), 0.000_001);
    }

    @Test
    void rejectsZeroOrNegativeCountsInsteadOfChangingTheRatio() {
        assertTrue(SilentGearAlloyReader.readMaterials(alloy(
                material("silentgear:iron", 0, NumericWidth.INT))).isEmpty());
        assertTrue(SilentGearAlloyReader.readMaterials(alloy(
                material("silentgear:iron", -1, NumericWidth.INT))).isEmpty());
    }

    private static CompoundTag alloy(CompoundTag... entries) {
        ListTag materials = new ListTag();
        for (CompoundTag entry : entries) materials.add(entry);
        CompoundTag root = new CompoundTag();
        root.put("Materials", materials);
        return root;
    }

    private static CompoundTag material(String materialId, long count, NumericWidth width) {
        CompoundTag material = new CompoundTag();
        material.putString("ID", materialId);
        switch (width) {
            case BYTE -> material.putByte("Count", (byte) count);
            case SHORT -> material.putShort("Count", (short) count);
            case INT -> material.putInt("Count", (int) count);
            case LONG -> material.putLong("Count", count);
        }
        return material;
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private enum NumericWidth { BYTE, SHORT, INT, LONG }
}
