package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Mutable staging index used by discovery adapters. */
public final class MaterialCorrelationIndex {
    private static final Set<ResourceLocation> NON_PHYSICAL_SENTINELS = Set.of(
            new ResourceLocation("minecraft", "air"),
            new ResourceLocation("minecraft", "barrier"),
            new ResourceLocation("minecraft", "structure_void")
    );

    private final Map<ResourceLocation, Candidate> byPhysicalItem = new LinkedHashMap<>();
    private final Map<ProfileKey, Set<ResourceLocation>> aliasesByProfile = new LinkedHashMap<>();

    public void accept(ResourceLocation physicalItem, MaterialProfile profile) {
        if (!isConcretePhysicalItem(physicalItem)) return;
        byPhysicalItem.computeIfAbsent(physicalItem, Candidate::new).put(profile);
        aliasesByProfile.computeIfAbsent(ProfileKey.of(profile), ignored -> new LinkedHashSet<>()).add(physicalItem);
    }

    public void acceptAll(Collection<ResourceLocation> physicalItems, MaterialProfile profile) {
        for (ResourceLocation physicalItem : physicalItems) accept(physicalItem, profile);
    }

    private static boolean isConcretePhysicalItem(ResourceLocation physicalItem) {
        return physicalItem != null && !NON_PHYSICAL_SENTINELS.contains(physicalItem);
    }

    public Optional<Candidate> get(ResourceLocation physicalItem) { return Optional.ofNullable(byPhysicalItem.get(physicalItem)); }
    public Collection<Candidate> all() { return java.util.List.copyOf(byPhysicalItem.values()); }
    public Set<ResourceLocation> aliases(MaterialProfile profile) {
        return Set.copyOf(aliasesByProfile.getOrDefault(ProfileKey.of(profile), Set.of()));
    }

    private record ProfileKey(MaterialProfile.Ecosystem ecosystem, ResourceLocation materialId) {
        private static ProfileKey of(MaterialProfile profile) { return new ProfileKey(profile.ecosystem(), profile.materialId()); }
    }

    public static final class Candidate {
        private final ResourceLocation physicalItem;
        private final Map<MaterialProfile.Ecosystem, LinkedHashMap<ResourceLocation, MaterialProfile>> claims =
                new EnumMap<>(MaterialProfile.Ecosystem.class);

        private Candidate(ResourceLocation physicalItem) { this.physicalItem = physicalItem; }

        private void put(MaterialProfile profile) {
            claims.computeIfAbsent(profile.ecosystem(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(profile.materialId(), profile);
        }

        public ResourceLocation physicalItem() { return physicalItem; }
        public ResourceLocation physicalTag() { return physicalItem; }

        /** Unique claims only. Ambiguous ecosystems are deliberately omitted. */
        public Map<MaterialProfile.Ecosystem, MaterialProfile> profiles() {
            Map<MaterialProfile.Ecosystem, MaterialProfile> result = new EnumMap<>(MaterialProfile.Ecosystem.class);
            for (var entry : claims.entrySet()) {
                if (entry.getValue().size() == 1) result.put(entry.getKey(), entry.getValue().values().iterator().next());
            }
            return Map.copyOf(result);
        }

        public Map<MaterialProfile.Ecosystem, Set<ResourceLocation>> claimIds() {
            Map<MaterialProfile.Ecosystem, Set<ResourceLocation>> result = new EnumMap<>(MaterialProfile.Ecosystem.class);
            for (var entry : claims.entrySet()) result.put(entry.getKey(), Set.copyOf(entry.getValue().keySet()));
            return Map.copyOf(result);
        }

        public boolean ambiguous() { return claims.values().stream().anyMatch(group -> group.size() > 1); }
        public boolean correlated() {
            return !ambiguous() && claims.containsKey(MaterialProfile.Ecosystem.SILENT_GEAR)
                    && claims.containsKey(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT);
        }
        public boolean bridgeCandidate() { return !ambiguous() && claims.size() == 1; }

        public UnifiedMaterial toUnified(ResourceLocation canonicalId) {
            if (ambiguous()) throw new IllegalStateException("Cannot unify ambiguous physical item " + physicalItem);
            return new UnifiedMaterial(canonicalId, physicalItem, profiles());
        }
    }
}
