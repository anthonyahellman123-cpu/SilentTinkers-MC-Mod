package com.anthonyahellman.silenttinkers.compat.silentgear;

import com.anthonyahellman.silenttinkers.material.MaterialCorrelationIndex;
import com.anthonyahellman.silenttinkers.material.MaterialProfile;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SilentGearDiscoveryBridgeTest {
    @Test
    void discoveredTraitIdentitySurvivesIntoUnifiedProfile() {
        ResourceLocation material = id("silentcompat:elementium");
        ResourceLocation item = id("botania:elementium_ingot");
        List<ResourceLocation> traits = List.of(id("silentgear:malleable"), id("silentcompat:pixie"));
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();

        SilentGearDiscoveryBridge.populate(index, () -> List.of(
                new SilentGearDiscoveryBridge.Entry(material, Set.of(item), traits)));

        MaterialProfile profile = index.get(item).orElseThrow().profiles()
                .get(MaterialProfile.Ecosystem.SILENT_GEAR);
        assertEquals(material, profile.materialId());
        assertEquals(traits, profile.traits());
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
