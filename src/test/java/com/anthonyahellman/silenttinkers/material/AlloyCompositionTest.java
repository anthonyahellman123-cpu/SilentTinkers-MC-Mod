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
    void rejectsUnsupportedPayloadVersion() {
        CompoundTag invalid = AlloyComposition.of(alloy(7, 2, 1)).save();
        invalid.putInt("Version", AlloyComposition.DATA_VERSION + 1);

        assertThrows(IllegalArgumentException.class, () -> AlloyComposition.load(invalid));
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
