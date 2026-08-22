package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mutable staging index used by discovery adapters.
 *
 * <p>Adapters may discover Silent Gear and Tinkers profiles independently.
 * The index correlates them by a shared physical tag without forcing either
 * ecosystem to surrender its own material ID or behavior.</p>
 */
public final class MaterialCorrelationIndex {
    private final Map<ResourceLocation, Candidate> byPhysicalTag = new LinkedHashMap<>();

    public void accept(ResourceLocation physicalTag, MaterialProfile profile) {
        byPhysicalTag.computeIfAbsent(physicalTag, Candidate::new).put(profile);
    }

    public Optional<Candidate> get(ResourceLocation physicalTag) {
        return Optional.ofNullable(byPhysicalTag.get(physicalTag));
    }

    public Collection<Candidate> all() {
        return java.util.List.copyOf(byPhysicalTag.values());
    }

    public static final class Candidate {
        private final ResourceLocation physicalTag;
        private final Map<MaterialProfile.Ecosystem, MaterialProfile> profiles =
                new java.util.EnumMap<>(MaterialProfile.Ecosystem.class);

        private Candidate(ResourceLocation physicalTag) {
            this.physicalTag = physicalTag;
        }

        private void put(MaterialProfile profile) {
            MaterialProfile previous = profiles.putIfAbsent(profile.ecosystem(), profile);
            if (previous != null && !previous.materialId().equals(profile.materialId())) {
                throw new IllegalStateException("Physical tag " + physicalTag
                        + " maps to multiple " + profile.ecosystem() + " materials: "
                        + previous.materialId() + " and " + profile.materialId());
            }
        }

        public ResourceLocation physicalTag() {
            return physicalTag;
        }

        public Map<MaterialProfile.Ecosystem, MaterialProfile> profiles() {
            return Map.copyOf(profiles);
        }

        public boolean correlated() {
            return profiles.containsKey(MaterialProfile.Ecosystem.SILENT_GEAR)
                    && profiles.containsKey(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT);
        }

        public boolean bridgeCandidate() {
            return profiles.size() == 1;
        }

        public UnifiedMaterial toUnified(ResourceLocation canonicalId) {
            return new UnifiedMaterial(canonicalId, physicalTag, profiles);
        }
    }
}
