package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/** A material's identity inside one authoritative equipment ecosystem. */
public record MaterialProfile(
        Ecosystem ecosystem,
        ResourceLocation materialId,
        List<ResourceLocation> traits) {

    public MaterialProfile {
        Objects.requireNonNull(ecosystem, "ecosystem");
        Objects.requireNonNull(materialId, "materialId");
        traits = traits == null ? List.of() : List.copyOf(traits);
    }

    public enum Ecosystem {
        SILENT_GEAR,
        TINKERS_CONSTRUCT
    }
}
