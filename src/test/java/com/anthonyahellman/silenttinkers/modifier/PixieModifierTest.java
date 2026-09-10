package com.anthonyahellman.silenttinkers.modifier;

import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PixieModifierTest {
    @Test
    void percentageLevelsScaleToSilentCompatFullHandChances() {
        assertEquals(0.0125, PixieModifier.chanceForLevel(1, EquipmentSlot.MAINHAND), 0.000_000_1);
        assertEquals(0.0250, PixieModifier.chanceForLevel(2, EquipmentSlot.MAINHAND), 0.000_000_1);
        assertEquals(0.0375, PixieModifier.chanceForLevel(3, EquipmentSlot.MAINHAND), 0.000_000_1);
        assertEquals(0.0500, PixieModifier.chanceForLevel(4, EquipmentSlot.MAINHAND), 0.000_000_1);
        assertEquals(0.0250, PixieModifier.chanceForLevel(4, EquipmentSlot.OFFHAND), 0.000_000_1);
    }
}
