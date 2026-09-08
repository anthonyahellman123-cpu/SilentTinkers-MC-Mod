package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical SilentTinkers view of one physical material.
 *
 * <p>The physical identity is intentionally separate from ecosystem profiles:
 * iron is one material even when Silent Gear and Tinkers assign different
 * stats or trait implementations to it.</p>
 */
public final class UnifiedMaterial {
    private final ResourceLocation canonicalId;
    private final ResourceLocation itemTag;
    private final Map<MaterialProfile.Ecosystem, MaterialProfile> profiles;

    public UnifiedMaterial(ResourceLocation canonicalId, ResourceLocation itemTag,
                           Map<MaterialProfile.Ecosystem, MaterialProfile> profiles) {
        this.canonicalId = Objects.requireNonNull(canonicalId, "canonicalId");
        this.itemTag = Objects.requireNonNull(itemTag, "itemTag");
        EnumMap<MaterialProfile.Ecosystem, MaterialProfile> copy =
                new EnumMap<>(MaterialProfile.Ecosystem.class);
        if (profiles != null) copy.putAll(profiles);
        this.profiles = Map.copyOf(copy);
    }

    public ResourceLocation canonicalId() { return canonicalId; }
    public ResourceLocation itemTag() { return itemTag; }
    public Map<MaterialProfile.Ecosystem, MaterialProfile> profiles() { return profiles; }

    public Optional<MaterialProfile> profile(MaterialProfile.Ecosystem ecosystem) {
        return Optional.ofNullable(profiles.get(ecosystem));
    }

    public boolean hasProfile(MaterialProfile.Ecosystem ecosystem) {
        return profiles.containsKey(ecosystem);
    }

    public boolean isBridgeCandidate() {
        return profiles.size() == 1;
    }
}
