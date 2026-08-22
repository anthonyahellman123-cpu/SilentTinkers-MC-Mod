package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Converts discovery decisions into explicit generation work. */
public final class MaterialGenerationPlanner {
    private MaterialGenerationPlanner() {}

    public static List<MaterialGenerationRequest> fromDiscovery(UnifiedMaterialDiscovery.Snapshot snapshot) {
        List<MaterialGenerationRequest> requests = new ArrayList<>();
        for (MaterialBridgePlan plan : MaterialBridgePlanner.plan(snapshot)) {
            requests.add(fromPlan(snapshot, plan, Optional.empty()));
        }
        requests.sort(Comparator.comparing(request -> request.physicalItem().toString()));
        return List.copyOf(requests);
    }

    public static MaterialGenerationRequest bootstrap(ResourceLocation physicalItem, int detectedMiningTier) {
        MaterialBridgePlan plan = MaterialBridgePlanner.bootstrap(physicalItem);
        BootstrapMaterialProfile profile = BootstrapMaterialResolver.resolve(physicalItem, detectedMiningTier);
        return new MaterialGenerationRequest(plan.physicalItem(), plan.action(), plan.source(), plan.target(),
                Optional.empty(), Optional.of(profile));
    }

    private static MaterialGenerationRequest fromPlan(UnifiedMaterialDiscovery.Snapshot snapshot,
            MaterialBridgePlan plan, Optional<BootstrapMaterialProfile> bootstrapProfile) {
        Optional<ResourceLocation> sourceMaterialId = plan.source().flatMap(source ->
                snapshot.index().get(plan.physicalItem())
                        .flatMap(candidate -> Optional.ofNullable(candidate.profiles().get(source)))
                        .map(MaterialProfile::materialId));

        // Canonical representative aliases may not themselves be the alias that
        // originally produced the plan. Resolve by scanning the source profile's
        // alias set when direct lookup is insufficient.
        if (sourceMaterialId.isEmpty() && plan.source().isPresent()) {
            MaterialProfile.Ecosystem source = plan.source().get();
            sourceMaterialId = snapshot.bridgeCandidates().stream()
                    .map(candidate -> candidate.profiles().get(source))
                    .filter(java.util.Objects::nonNull)
                    .filter(profile -> snapshot.index().aliases(profile).contains(plan.physicalItem()))
                    .map(MaterialProfile::materialId)
                    .findFirst();
        }

        return new MaterialGenerationRequest(plan.physicalItem(), plan.action(), plan.source(), plan.target(),
                sourceMaterialId, bootstrapProfile);
    }
}
