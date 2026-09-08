package com.anthonyahellman.silenttinkers.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void visualSourceCanBeReadFromTheSharedAlloyEnvelope() {
        SourceVisualIdentity visual = new SourceVisualIdentity(
                id("botania:elementium_ingot"), Optional.empty());
        CompoundTag root = AlloyComposition.of(java.util.Map.of(id("silentcompat:elementium"), 1L)).save();
        root.put("VisualSource", visual.save());
        CompoundTag carrier = new CompoundTag();
        carrier.put(AlloyPayload.ROOT_KEY, root);

        assertEquals(visual.itemId(), AlloyPayload.readVisualSource(carrier).orElseThrow().itemId());
    }

    @Test
    void toolCastingRequiresCompositionAndEvaluatedStats() {
        CompoundTag root = AlloyComposition.of(java.util.Map.of(id("silentgear:iron"), 1L)).save();
        CompoundTag carrier = new CompoundTag();
        carrier.put(AlloyPayload.ROOT_KEY, root);

        assertFalse(AlloyPayload.isToolCastReady(carrier));

        root.put("EvaluatedStats", new AlloyStatSnapshot(
                512f, 7f, 3f, 0f, id("minecraft:iron")).save());
        carrier.put(AlloyPayload.ROOT_KEY, root);

        assertTrue(AlloyPayload.isToolCastReady(carrier));
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
