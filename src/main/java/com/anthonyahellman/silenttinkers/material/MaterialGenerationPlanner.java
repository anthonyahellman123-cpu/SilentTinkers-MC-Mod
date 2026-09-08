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
            requests.add(fromPlan(plan, Optional.empty()));
        }
        requests.sort(Comparator.comparing(request -> request.physicalItem().toString()));
        return List.copyOf(requests);
    }

    public static MaterialGenerationRequest bootstrap(ResourceLocation physicalItem, int detectedMiningTier) {
        MaterialBridgePlan plan = MaterialBridgePlanner.bootstrap(physicalItem);
        BootstrapMaterialProfile profile = BootstrapMaterialResolver.resolve(physicalItem, detectedMiningTier);
        return new MaterialGenerationRequest(plan.physicalItem(), plan.action(), plan.source(), plan.target(),
                plan.sourceMaterialId(), plan.targetMaterialId(), Optional.of(profile));
    }

    private static MaterialGenerationRequest fromPlan(MaterialBridgePlan plan,
            Optional<BootstrapMaterialProfile> bootstrapProfile) {
        return new MaterialGenerationRequest(plan.physicalItem(), plan.action(), plan.source(), plan.target(),
                plan.sourceMaterialId(), plan.targetMaterialId(), bootstrapProfile);
    }
}
