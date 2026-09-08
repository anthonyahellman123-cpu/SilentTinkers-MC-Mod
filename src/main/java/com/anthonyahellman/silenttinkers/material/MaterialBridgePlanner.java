package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Turns a discovery snapshot into explicit, non-destructive bridge decisions. */
public final class MaterialBridgePlanner {
    private MaterialBridgePlanner() {}

    public static List<MaterialBridgePlan> plan(UnifiedMaterialDiscovery.Snapshot snapshot) {
        Map<PlanKey, MaterialBridgePlan> plans = new LinkedHashMap<>();
        Set<ProfileKey> correlatedProfiles = new LinkedHashSet<>();

        for (MaterialCorrelationIndex.Candidate candidate : snapshot.correlated()) {
            Map<MaterialProfile.Ecosystem, MaterialProfile> profiles = candidate.profiles();
            MaterialProfile sg = profiles.get(MaterialProfile.Ecosystem.SILENT_GEAR);
            MaterialProfile tc = profiles.get(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT);
            correlatedProfiles.add(ProfileKey.of(sg));
            correlatedProfiles.add(ProfileKey.of(tc));

            PlanKey key = PlanKey.correlated(sg.materialId(), tc.materialId());
            plans.putIfAbsent(key, new MaterialBridgePlan(
                    canonicalPhysicalItem(snapshot, candidate, sg, tc),
                    MaterialBridgePlan.Action.PRESERVE,
                    Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(),
                    MaterialBridgePlan.Reason.BOTH_PRESENT));
        }

        for (MaterialCorrelationIndex.Candidate candidate : snapshot.bridgeCandidates()) {
            Map<MaterialProfile.Ecosystem, MaterialProfile> profiles = candidate.profiles();
            MaterialProfile profile = profiles.values().iterator().next();
            if (correlatedProfiles.contains(ProfileKey.of(profile))) continue;
            // A one-sided profile bearing our deterministic ownership ID is a
            // previously generated target, never a new foreign source.
            if (GeneratedMaterialOwnership.isOwned(profile.materialId())) continue;

            PlanKey key = PlanKey.single(profile.ecosystem(), profile.materialId());
            MaterialProfile.Ecosystem target = profile.ecosystem() == MaterialProfile.Ecosystem.SILENT_GEAR
                    ? MaterialProfile.Ecosystem.TINKERS_CONSTRUCT
                    : MaterialProfile.Ecosystem.SILENT_GEAR;
            MaterialBridgePlan.Reason reason = profile.ecosystem() == MaterialProfile.Ecosystem.SILENT_GEAR
                    ? MaterialBridgePlan.Reason.SILENT_GEAR_ONLY
                    : MaterialBridgePlan.Reason.TINKERS_ONLY;

            plans.putIfAbsent(key, new MaterialBridgePlan(
                    canonicalPhysicalItem(snapshot, candidate, profile),
                    MaterialBridgePlan.Action.BRIDGE,
                    Optional.of(profile.ecosystem()), Optional.of(target),
                    Optional.of(profile.materialId()),
                    Optional.of(GeneratedMaterialOwnership.idFor(target, profile.materialId())),
                    reason));
        }

        List<MaterialBridgePlan> result = new ArrayList<>(plans.values());
        result.sort(Comparator.comparing(plan -> plan.physicalItem().toString()));
        return List.copyOf(result);
    }

    private static ResourceLocation canonicalPhysicalItem(UnifiedMaterialDiscovery.Snapshot snapshot,
            MaterialCorrelationIndex.Candidate fallback, MaterialProfile... profiles) {
        Set<ResourceLocation> aliases = new LinkedHashSet<>();
        for (MaterialProfile profile : profiles) aliases.addAll(snapshot.index().aliases(profile));

        return aliases.stream()
                .filter(alias -> snapshot.index().get(alias)
                        .map(candidate -> !candidate.ambiguous())
                        .orElse(false))
                .sorted(Comparator.comparingInt(MaterialBridgePlanner::aliasPriority)
                        .thenComparing(ResourceLocation::toString))
                .findFirst()
                .orElse(fallback.physicalItem());
    }

    private static int aliasPriority(ResourceLocation id) {
        String path = id.getPath();
        if (path.contains("ingot")) return 0;
        if (path.contains("gem") || path.contains("crystal")) return 1;
        if (path.contains("nugget") || path.startsWith("block_") || path.endsWith("_block")) return 3;
        return 2;
    }

    private record ProfileKey(MaterialProfile.Ecosystem ecosystem, ResourceLocation materialId) {
        static ProfileKey of(MaterialProfile profile) {
            return new ProfileKey(profile.ecosystem(), profile.materialId());
        }
    }

    private record PlanKey(MaterialProfile.Ecosystem ecosystem, ResourceLocation first, ResourceLocation second) {
        static PlanKey single(MaterialProfile.Ecosystem ecosystem, ResourceLocation id) {
            return new PlanKey(ecosystem, id, null);
        }
        static PlanKey correlated(ResourceLocation sg, ResourceLocation tc) {
            return new PlanKey(null, sg, tc);
        }
    }

    public static MaterialBridgePlan bootstrap(ResourceLocation physicalItem) {
        return new MaterialBridgePlan(physicalItem, MaterialBridgePlan.Action.BOOTSTRAP,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                MaterialBridgePlan.Reason.EXTERNAL_MATERIAL);
    }
}
