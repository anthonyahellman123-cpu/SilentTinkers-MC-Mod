package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicBridgeItemPolicyTest {
    @Test
    void acceptsObviousOneUnitMaterialForms() {
        assertTrue(DynamicBridgeItemPolicy.isOneUnitMaterial(id("example:bronze_ingot")));
        assertTrue(DynamicBridgeItemPolicy.isOneUnitMaterial(id("example:ingot_bronze")));
        assertTrue(DynamicBridgeItemPolicy.isOneUnitMaterial(id("example:ruby_gem")));
        assertTrue(DynamicBridgeItemPolicy.isOneUnitMaterial(id("example:mana_crystal")));
    }

    @Test
    void rejectsFormsWithUnknownOrDifferentMaterialAmounts() {
        assertFalse(DynamicBridgeItemPolicy.isOneUnitMaterial(id("example:bronze_block")));
        assertFalse(DynamicBridgeItemPolicy.isOneUnitMaterial(id("example:bronze_nugget")));
        assertFalse(DynamicBridgeItemPolicy.isOneUnitMaterial(id("minecraft:oak_planks")));
        assertFalse(DynamicBridgeItemPolicy.isOneUnitMaterial(id("example:bronze_plate")));
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
