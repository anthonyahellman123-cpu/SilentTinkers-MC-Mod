package com.anthonyahellman.silenttinkers.material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Turns a discovery snapshot into explicit, non-destructive bridge decisions. */
public final class MaterialBridgePlanner {
    private MaterialBridgePlanner() {}

    public static List<MaterialBridgePlan> plan(UnifiedMaterialDiscovery.Snapshot snapshot) {
        List<MaterialBridgePlan> plans = new ArrayList<>();

        for (MaterialCorrelationIndex.Candidate candidate : snapshot.correlated()) {
            plans.add(new MaterialBridgePlan(
                    candidate.physicalItem(),
                    MaterialBridgePlan.Action.PRESERVE,
                    Optional.empty(),
                    Optional.empty(),
                    MaterialBridgePlan.Reason.BOTH_PRESENT));
        }

        for (MaterialCorrelationIndex.Candidate candidate : snapshot.bridgeCandidates()) {
            Map<MaterialProfile.Ecosystem, MaterialProfile> profiles = candidate.profiles();
            if (profiles.containsKey(MaterialProfile.Ecosystem.SILENT_GEAR)) {
                plans.add(new MaterialBridgePlan(
                        candidate.physicalItem(),
                        MaterialBridgePlan.Action.BRIDGE,
                        Optional.of(MaterialProfile.Ecosystem.SILENT_GEAR),
                        Optional.of(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT),
                        MaterialBridgePlan.Reason.SILENT_GEAR_ONLY));
            } else if (profiles.containsKey(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT)) {
                plans.add(new MaterialBridgePlan(
                        candidate.physicalItem(),
                        MaterialBridgePlan.Action.BRIDGE,
                        Optional.of(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT),
                        Optional.of(MaterialProfile.Ecosystem.SILENT_GEAR),
                        MaterialBridgePlan.Reason.TINKERS_ONLY));
            }
        }

        plans.sort(Comparator.comparing(plan -> plan.physicalItem().toString()));
        return List.copyOf(plans);
    }

    /**
     * External material discovery (Ice & Fire, Mekanism, etc.) will call this
     * when neither ecosystem has a profile. Mining-tier fallback belongs to the
     * later bootstrap policy, not to correlation itself.
     */
    public static MaterialBridgePlan bootstrap(net.minecraft.resources.ResourceLocation physicalItem) {
        return new MaterialBridgePlan(
                physicalItem,
                MaterialBridgePlan.Action.BOOTSTRAP,
                Optional.empty(),
                Optional.empty(),
                MaterialBridgePlan.Reason.EXTERNAL_MATERIAL);
    }
}
