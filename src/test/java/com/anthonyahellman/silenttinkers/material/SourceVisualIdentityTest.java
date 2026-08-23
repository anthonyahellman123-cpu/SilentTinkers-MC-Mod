package com.anthonyahellman.silenttinkers.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceVisualIdentityTest {
    @Test
    void sourceIdentityRoundTripPreservesItemAndDynamicTag() {
        CompoundTag sourceTag = new CompoundTag();
        sourceTag.putString("MaterialFingerprint", "elementium-test");
        SourceVisualIdentity original = new SourceVisualIdentity(
                id("botania:elementium_ingot"), Optional.of(sourceTag));

        SourceVisualIdentity restored = SourceVisualIdentity.load(original.save()).orElseThrow();

        assertEquals(id("botania:elementium_ingot"), restored.itemId());
        assertEquals("elementium-test", restored.itemTag().orElseThrow().getString("MaterialFingerprint"));
    }

    @Test
    void visualSourceSurvivesTheSharedAlloyEnvelope() {
        AlloyComposition composition = AlloyComposition.of(Map.of(id("silentcompat:elementium"), 1L));
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                720.0f, 6.2f, 2.0f, 0.0f, id("minecraft:diamond"));
        SourceVisualIdentity visual = new SourceVisualIdentity(
                id("botania:elementium_ingot"), Optional.empty());
        ItemStack carrier = new ItemStack(Items.STICK);

        AlloyPayload.write(carrier, composition, 0, Optional.of(stats), Optional.of(visual));

        assertEquals(composition.fingerprint(), AlloyPayload.read(carrier).orElseThrow().fingerprint());
        assertEquals(stats, AlloyPayload.readStats(carrier).orElseThrow());
        assertEquals(visual.itemId(), AlloyPayload.readVisualSource(carrier).orElseThrow().itemId());
    }

    @Test
    void sourceTagAccessIsDefensive() {
        CompoundTag sourceTag = new CompoundTag();
        sourceTag.putInt("ColorSeed", 42);
        SourceVisualIdentity visual = new SourceVisualIdentity(id("silentgear:alloy_ingot"), Optional.of(sourceTag));

        CompoundTag exposed = visual.itemTag().orElseThrow();
        exposed.putInt("ColorSeed", 99);

        assertEquals(42, visual.itemTag().orElseThrow().getInt("ColorSeed"));
        assertTrue(visual.save().contains("Tag"));
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
