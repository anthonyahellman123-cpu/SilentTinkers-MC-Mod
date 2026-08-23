package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AlloyVariantCodecTest {
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
}
