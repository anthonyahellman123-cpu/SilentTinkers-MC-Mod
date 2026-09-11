package com.anthonyahellman.silenttinkers.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlloyCompositionTest {
    @Test
    void equivalentRatiosHaveOneIdentity() {
        AlloyComposition large = AlloyComposition.of(alloy(70, 20, 10));
        AlloyComposition reduced = AlloyComposition.of(alloy(7, 2, 1));

        assertEquals(reduced.ingredients(), large.ingredients());
        assertEquals(reduced.fingerprint(), large.fingerprint());
        assertEquals(0.7, reduced.fraction(id("silentgear:iron")), 0.000_001);
    }

    @Test
    void equalFourWayAlloyPreservesExactQuarterSharesAndStats() {
        Map<ResourceLocation, Long> fourWay = new LinkedHashMap<>();
        fourWay.put(id("silentgear:iron"), 25L);
        fourWay.put(id("tcompat:calorite"), 25L);
        fourWay.put(id("tinkers_advanced:antimony"), 25L);
        fourWay.put(id("silentcompat:elementium"), 25L);

        AlloyComposition original = AlloyComposition.of(fourWay);
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                2048.0f, 12.5f, 8.25f, 0.15f, id("minecraft:netherite"));
        String encoded = AlloyVariantCodec.encode(original, 0, java.util.Optional.of(stats));
        AlloyComposition restored = AlloyVariantCodec.decode(encoded);

        assertEquals(4, restored.ingredients().size());
        assertEquals(0.25, restored.fraction(id("silentgear:iron")), 0.000_001);
        assertEquals(0.25, restored.fraction(id("tcompat:calorite")), 0.000_001);
        assertEquals(0.25, restored.fraction(id("tinkers_advanced:antimony")), 0.000_001);
        assertEquals(0.25, restored.fraction(id("silentcompat:elementium")), 0.000_001);
        assertEquals(original.fingerprint(), restored.fingerprint());
        assertEquals(stats, AlloyVariantCodec.decodeStats(encoded).orElseThrow());
    }

    @Test
    void nbtRoundTripPreservesCanonicalIdentity() {
        AlloyComposition original = AlloyComposition.of(alloy(7, 2, 1));
        AlloyComposition restored = AlloyComposition.load(original.save());

        assertEquals(original.ingredients(), restored.ingredients());
        assertEquals(original.fingerprint(), restored.fingerprint());
    }

    @Test
    void materialVariantRoundTripPreservesCanonicalIdentity() {
        AlloyComposition original = AlloyComposition.of(alloy(7, 2, 1));
        AlloyComposition restored = AlloyVariantCodec.decode(AlloyVariantCodec.encode(original));

        assertEquals(original.ingredients(), restored.ingredients());
        assertEquals(original.fingerprint(), restored.fingerprint());
    }

    @Test
    void chargedMaterialVariantPreservesChargeAndComposition() {
        AlloyComposition original = AlloyComposition.of(alloy(7, 2, 1));
        String encoded = AlloyVariantCodec.encode(original, 3);

        assertEquals(original.fingerprint(), AlloyVariantCodec.decode(encoded).fingerprint());
        assertEquals(3, AlloyVariantCodec.decodeStarChargeLevel(encoded));
    }

    @Test
    void evaluatedStatsSurviveMaterialVariantRoundTrip() {
        AlloyComposition composition = AlloyComposition.of(alloy(7, 2, 1));
        AlloyStatSnapshot stats = new AlloyStatSnapshot(
                812.5f, 11.25f, 4.75f, 0.2f, id("minecraft:diamond"));
        String encoded = AlloyVariantCodec.encode(composition, 2, java.util.Optional.of(stats));

        assertEquals(composition.fingerprint(), AlloyVariantCodec.decode(encoded).fingerprint());
        assertEquals(2, AlloyVariantCodec.decodeStarChargeLevel(encoded));
        assertEquals(stats, AlloyVariantCodec.decodeStats(encoded).orElseThrow());
    }

    @Test
    void rejectsUnsupportedPayloadVersion() {
        CompoundTag invalid = AlloyComposition.of(alloy(7, 2, 1)).save();
        invalid.putInt("Version", AlloyComposition.DATA_VERSION + 1);

        assertThrows(IllegalArgumentException.class, () -> AlloyComposition.load(invalid));
    }

    @Test
    void rejectsZeroAndMalformedCompositionUnits() {
        Map<ResourceLocation, Long> zero = new LinkedHashMap<>();
        zero.put(id("silentgear:iron"), 0L);
        assertThrows(IllegalArgumentException.class, () -> AlloyComposition.of(zero));
        assertThrows(IllegalArgumentException.class, () -> AlloyComposition.of(Map.of()));
    }

    private static Map<ResourceLocation, Long> alloy(long iron, long redstone, long diamond) {
        Map<ResourceLocation, Long> result = new LinkedHashMap<>();
        result.put(id("silentgear:iron"), iron);
        result.put(id("silentgear:redstone"), redstone);
        result.put(id("silentgear:diamond"), diamond);
        return result;
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
