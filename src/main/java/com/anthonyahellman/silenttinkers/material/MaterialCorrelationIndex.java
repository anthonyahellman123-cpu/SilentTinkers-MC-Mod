package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable staging index used by discovery adapters.
 *
 * <p>Discovery is allowed to report several physical aliases for one native
 * material (for example multiple ingots accepted by a tag-backed recipe).
 * Aliases are indexed together first; cross-ecosystem correlation only occurs
 * when both sides actually share a physical item identity.</p>
 */
public final class MaterialCorrelationIndex {
    private final Map<ResourceLocation, Candidate> byPhysicalItem = new LinkedHashMap<>();
    private final Map<ProfileKey, Set<ResourceLocation>> aliasesByProfile = new LinkedHashMap<>();

    public void accept(ResourceLocation physicalItem, MaterialProfile profile) {
        byPhysicalItem.computeIfAbsent(physicalItem, Candidate::new).put(profile);
        aliasesByProfile.computeIfAbsent(ProfileKey.of(profile), ignored -> new LinkedHashSet<>())
                .add(physicalItem);
    }

    /** Adds every concrete physical alias discovered for one ecosystem profile. */
    public void acceptAll(Collection<ResourceLocation> physicalItems, MaterialProfile profile) {
        for (ResourceLocation physicalItem : physicalItems) {
            accept(physicalItem, profile);
        }
    }

    public Optional<Candidate> get(ResourceLocation physicalItem) {
        return Optional.ofNullable(byPhysicalItem.get(physicalItem));
    }

    public Collection<Candidate> all() {
        return java.util.List.copyOf(byPhysicalItem.values());
    }

    /** Returns all concrete item aliases seen for a native material profile. */
    public Set<ResourceLocation> aliases(MaterialProfile profile) {
        return Set.copyOf(aliasesByProfile.getOrDefault(ProfileKey.of(profile), Set.of()));
    }

    private record ProfileKey(MaterialProfile.Ecosystem ecosystem, ResourceLocation materialId) {
        private static ProfileKey of(MaterialProfile profile) {
            return new ProfileKey(profile.ecosystem(), profile.materialId());
        }
    }

    public static final class Candidate {
        private final ResourceLocation physicalItem;
        private final Map<MaterialProfile.Ecosystem, MaterialProfile> profiles =
                new EnumMap<>(MaterialProfile.Ecosystem.class);

        private Candidate(ResourceLocation physicalItem) {
            this.physicalItem = physicalItem;
        }

        private void put(MaterialProfile profile) {
            MaterialProfile previous = profiles.putIfAbsent(profile.ecosystem(), profile);
            if (previous != null && !previous.materialId().equals(profile.materialId())) {
                throw new IllegalStateException("Physical item " + physicalItem
                        + " maps to multiple " + profile.ecosystem() + " materials: "
                        + previous.materialId() + " and " + profile.materialId());
            }
        }

        public ResourceLocation physicalItem() {
            return physicalItem;
        }

        /** Compatibility alias while UnifiedMaterial still names this field itemTag. */
        public ResourceLocation physicalTag() {
            return physicalItem;
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
            return new UnifiedMaterial(canonicalId, physicalItem, profiles);
        }
    }
}
