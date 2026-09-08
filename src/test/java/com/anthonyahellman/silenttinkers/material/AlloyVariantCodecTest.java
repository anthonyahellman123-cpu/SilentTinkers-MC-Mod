package com.anthonyahellman.silenttinkers.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AlloyVariantCodecTest {
    @Test
    void validCompositeDecodesExactStats() {
        AlloyComposition composition = AlloyComposition.of(Map.of(
                new ResourceLocation("silentgear", "azure_electrum"), 1L));
        AlloyStatSnapshot expected = new AlloyStatSnapshot(
                1536.0f, 11.75f, 6.5f, -0.15f,
                new ResourceLocation("minecraft", "netherite"));

        String encoded = AlloyVariantCodec.encode(composition, 0, Optional.of(expected));

        assertTrue(AlloyVariantCodec.isEncodedVariant(encoded));
        assertEquals(expected, AlloyVariantCodec.decodeStats(encoded).orElseThrow());
    }

    @Test
    void baseDisplayVariantIsNotAnEncodedCompositePayload() {
        String displayVariant = "silenttinkers:composite_alloy";

        assertFalse(AlloyVariantCodec.isEncodedVariant(displayVariant));
        assertThrows(IllegalArgumentException.class, () -> AlloyVariantCodec.decode(displayVariant));
        assertThrows(IllegalArgumentException.class, () -> AlloyVariantCodec.decodeStats(displayVariant));
    }

    @Test
    void compositionWithoutStatSuffixIsExplicitlyMissingStats() {
        AlloyComposition composition = AlloyComposition.of(Map.of(
                new ResourceLocation("silentgear", "iron"), 1L));
        String encoded = AlloyVariantCodec.encode(composition);

        assertTrue(AlloyVariantCodec.isEncodedVariant(encoded));
        assertTrue(AlloyVariantCodec.decodeStats(encoded).isEmpty());
    }

    @Test
    void thirtySeventyPercentagesSurviveFinishedToolNbtPersistence() {
        AlloyComposition composition = AlloyComposition.of(Map.of(
                new ResourceLocation("silentgear", "iron"), 30L,
                new ResourceLocation("silentgear", "redstone"), 70L));
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                640.0f, 8.5f, 3.25f, 0.0f,
                new ResourceLocation("minecraft", "diamond"));

        MaterialVariantId variant = MaterialVariantId.create(
                new MaterialId("silenttinkers", "composite_alloy"),
                AlloyVariantCodec.encode(composition, 0, Optional.of(stats)));
        MaterialNBT restored = MaterialNBT.readFromNBT(
                MaterialNBT.of(MaterialVariant.of(variant)).serializeToNBT());
        AlloyComposition decoded = AlloyVariantCodec.decode(
                restored.get(0).getVariant().getVariant());

        assertEquals(0.30, decoded.fraction(new ResourceLocation("silentgear", "iron")), 0.000_001);
        assertEquals(0.70, decoded.fraction(new ResourceLocation("silentgear", "redstone")), 0.000_001);
        assertEquals(composition.fingerprint(), decoded.fingerprint());
    }

    @Test
    void elementiumPayloadRoundTripsThroughVariantString() {
        ResourceLocation elementium = new ResourceLocation("silentcompat", "elementium");
        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        ingredients.put(elementium, 1L);
        AlloyComposition composition = AlloyComposition.of(ingredients);
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                720.0f, 6.2f, 2.0f, 0.0f,
                new ResourceLocation("minecraft", "diamond"));

        String encoded = AlloyVariantCodec.encode(composition, 0, Optional.of(stats));

        AlloyComposition decodedComposition = AlloyVariantCodec.decode(encoded);
        Optional<AlloyStatSnapshot> decodedStats = AlloyVariantCodec.decodeStats(encoded);

        assertEquals(composition.fingerprint(), decodedComposition.fingerprint());
        assertTrue(decodedStats.isPresent());
        assertEquals(stats, decodedStats.orElseThrow());
        assertEquals(0, AlloyVariantCodec.decodeStarChargeLevel(encoded));
    }

    @Test
    void encodedPayloadSurvivesTinkersMaterialVariantStringRoundTrip() {
        ResourceLocation elementium = new ResourceLocation("silentcompat", "elementium");
        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        ingredients.put(elementium, 1L);
        AlloyComposition composition = AlloyComposition.of(ingredients);
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                720.0f, 6.2f, 2.0f, 0.0f,
                new ResourceLocation("minecraft", "diamond"));

        String encoded = AlloyVariantCodec.encode(composition, 0, Optional.of(stats));
        MaterialId base = new MaterialId("silenttinkers", "composite_alloy");
        MaterialVariantId created = MaterialVariantId.create(base, encoded);
        MaterialVariantId reparsed = MaterialVariantId.tryParse(created.toString());

        assertNotNull(reparsed);
        assertTrue(created.sameVariant(reparsed));
        assertEquals(composition.fingerprint(), AlloyVariantCodec.decode(reparsed.getVariant()).fingerprint());
        assertEquals(stats, AlloyVariantCodec.decodeStats(reparsed.getVariant()).orElseThrow());
    }

    @Test
    void encodedPayloadSurvivesTinkersMaterialNbtPersistence() {
        ResourceLocation elementium = new ResourceLocation("silentcompat", "elementium");
        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        ingredients.put(elementium, 1L);
        AlloyComposition composition = AlloyComposition.of(ingredients);
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                720.0f, 6.2f, 2.0f, 0.0f,
                new ResourceLocation("minecraft", "diamond"));

        MaterialVariantId variant = MaterialVariantId.create(
                new MaterialId("silenttinkers", "composite_alloy"),
                AlloyVariantCodec.encode(composition, 0, Optional.of(stats)));
        MaterialNBT original = MaterialNBT.of(MaterialVariant.of(variant));
        ListTag serialized = original.serializeToNBT();
        MaterialNBT restored = MaterialNBT.readFromNBT(serialized);
        MaterialVariantId restoredVariant = restored.get(0).getVariant();

        assertTrue(variant.sameVariant(restoredVariant));
        assertEquals(composition.fingerprint(), AlloyVariantCodec.decode(restoredVariant.getVariant()).fingerprint());
        assertEquals(stats, AlloyVariantCodec.decodeStats(restoredVariant.getVariant()).orElseThrow());
    }

    @Test
    void starchargeAndStatsCanCoexistInSameVariant() {
        ResourceLocation material = new ResourceLocation("silentgear", "tyrian_steel");
        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        ingredients.put(material, 3L);
        AlloyComposition composition = AlloyComposition.of(ingredients);
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                3652.0f, 18.0f, 8.0f, -0.1f,
                new ResourceLocation("minecraft", "netherite"));

        String encoded = AlloyVariantCodec.encode(composition, 4, Optional.of(stats));

        assertEquals(composition.fingerprint(), AlloyVariantCodec.decode(encoded).fingerprint());
        assertEquals(4, AlloyVariantCodec.decodeStarChargeLevel(encoded));
        assertEquals(stats, AlloyVariantCodec.decodeStats(encoded).orElseThrow());
    }

    @Test
    void sourceItemIdentitySurvivesFinishedToolMaterialPersistence() {
        AlloyComposition composition = AlloyComposition.of(Map.of(
                new ResourceLocation("silentcompat", "elementium"), 1L));
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                720.0f, 6.2f, 2.0f, 0.0f,
                new ResourceLocation("minecraft", "diamond"));
        SourceVisualIdentity visualSource = new SourceVisualIdentity(
                new ResourceLocation("botania", "elementium_ingot"), Optional.empty());

        MaterialVariantId variant = MaterialVariantId.create(
                new MaterialId("silenttinkers", "composite_alloy"),
                AlloyVariantCodec.encode(composition, 0, Optional.of(stats), Optional.of(visualSource)));
        MaterialNBT restored = MaterialNBT.readFromNBT(
                MaterialNBT.of(MaterialVariant.of(variant)).serializeToNBT());
        String restoredVariant = restored.get(0).getVariant().getVariant();

        assertEquals(new ResourceLocation("botania", "elementium_ingot"),
                AlloyVariantCodec.decodeVisualSourceItemId(restoredVariant).orElseThrow());
        assertEquals(stats, AlloyVariantCodec.decodeStats(restoredVariant).orElseThrow());
        assertEquals(composition.fingerprint(), AlloyVariantCodec.decode(restoredVariant).fingerprint());
    }

    @Test
    void dynamicSourceVisualTagSurvivesFinishedToolMaterialPersistence() {
        AlloyComposition composition = AlloyComposition.of(Map.of(
                new ResourceLocation("silentgear", "alloy_ingot"), 1L));
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                1337.0f, 9.5f, 4.25f, 0.1f,
                new ResourceLocation("minecraft", "netherite"));
        CompoundTag dynamicTag = new CompoundTag();
        dynamicTag.putString("VisualFingerprint", "ratio:3-1-elementium-manasteel");
        dynamicTag.putInt("ColorSeed", 42);
        SourceVisualIdentity visualSource = new SourceVisualIdentity(
                new ResourceLocation("silentgear", "alloy_ingot"), Optional.of(dynamicTag));

        MaterialVariantId variant = MaterialVariantId.create(
                new MaterialId("silenttinkers", "composite_alloy"),
                AlloyVariantCodec.encode(composition, 2, Optional.of(stats), Optional.of(visualSource)));
        MaterialNBT restored = MaterialNBT.readFromNBT(
                MaterialNBT.of(MaterialVariant.of(variant)).serializeToNBT());
        String restoredVariant = restored.get(0).getVariant().getVariant();
        SourceVisualIdentity restoredVisual = AlloyVariantCodec.decodeVisualSource(restoredVariant).orElseThrow();

        assertEquals(visualSource.itemId(), restoredVisual.itemId());
        assertEquals("ratio:3-1-elementium-manasteel",
                restoredVisual.itemTag().orElseThrow().getString("VisualFingerprint"));
        assertEquals(42, restoredVisual.itemTag().orElseThrow().getInt("ColorSeed"));
        assertEquals(2, AlloyVariantCodec.decodeStarChargeLevel(restoredVariant));
        assertEquals(stats, AlloyVariantCodec.decodeStats(restoredVariant).orElseThrow());
    }

    @Test
    void threeMaterialCompositionAndStatsSurviveFinishedToolPersistence() {
        AlloyComposition composition = AlloyComposition.of(Map.of(
                new ResourceLocation("silentgear", "iron"), 1L,
                new ResourceLocation("silentcompat", "elementium"), 1L,
                new ResourceLocation("tinkers_advanced", "neutronium"), 1L));
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                16384.0f, 10.8f, 22.7f, 0.05f,
                new ResourceLocation("minecraft", "netherite"));
        MaterialVariantId original = MaterialVariantId.create(
                new MaterialId("silenttinkers", "composite_alloy"),
                AlloyVariantCodec.encode(composition, 0, Optional.of(stats)));

        MaterialNBT restored = MaterialNBT.readFromNBT(
                MaterialNBT.of(MaterialVariant.of(original)).serializeToNBT());
        String restoredVariant = restored.get(0).getVariant().getVariant();

        assertEquals(composition.fingerprint(), AlloyVariantCodec.decode(restoredVariant).fingerprint());
        assertEquals(stats, AlloyVariantCodec.decodeStats(restoredVariant).orElseThrow());
    }

    @Test
    void fourMaterialQuarterSharesAndStatsSurviveFinishedToolPersistence() {
        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        ingredients.put(new ResourceLocation("silentgear", "iron"), 25L);
        ingredients.put(new ResourceLocation("silentcompat", "elementium"), 25L);
        ingredients.put(new ResourceLocation("tinkers_advanced", "antimony"), 25L);
        ingredients.put(new ResourceLocation("tcompat", "calorite"), 25L);
        AlloyComposition composition = AlloyComposition.of(ingredients);
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                4096.0f, 14.25f, 11.5f, 0.2f,
                new ResourceLocation("minecraft", "netherite"));
        MaterialVariantId original = MaterialVariantId.create(
                new MaterialId("silenttinkers", "composite_alloy"),
                AlloyVariantCodec.encode(composition, 0, Optional.of(stats)));

        MaterialNBT restored = MaterialNBT.readFromNBT(
                MaterialNBT.of(MaterialVariant.of(original)).serializeToNBT());
        String restoredVariant = restored.get(0).getVariant().getVariant();
        AlloyComposition decoded = AlloyVariantCodec.decode(restoredVariant);

        assertEquals(4, decoded.ingredients().size());
        for (ResourceLocation material : ingredients.keySet()) {
            assertEquals(0.25, decoded.fraction(material), 0.000_001);
        }
        assertEquals(composition.fingerprint(), decoded.fingerprint());
        assertEquals(stats, AlloyVariantCodec.decodeStats(restoredVariant).orElseThrow());
    }
}
