package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearApiProvider;
import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearDiscoveryBridge;
import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersCorrelationAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds one correlation snapshot from both loaded material ecosystems. */
public final class UnifiedMaterialDiscovery {
    private UnifiedMaterialDiscovery() {}

    public static Snapshot discover() {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();

        SilentGearDiscoveryBridge.DiscoveryReport silentGear =
                SilentGearDiscoveryBridge.populate(index, new SilentGearApiProvider());
        TinkersCorrelationAdapter.DiscoveryReport tinkers =
                TinkersCorrelationAdapter.populate(index);

        List<MaterialCorrelationIndex.Candidate> correlated = new ArrayList<>();
        List<MaterialCorrelationIndex.Candidate> bridgeCandidates = new ArrayList<>();
        for (MaterialCorrelationIndex.Candidate candidate : index.all()) {
            if (candidate.correlated()) {
                correlated.add(candidate);
            } else if (candidate.bridgeCandidate()) {
                bridgeCandidates.add(candidate);
            }
        }

        Comparator<MaterialCorrelationIndex.Candidate> byItem =
                Comparator.comparing(candidate -> candidate.physicalItem().toString());
        correlated.sort(byItem);
        bridgeCandidates.sort(byItem);

        Snapshot snapshot = new Snapshot(index, silentGear, tinkers,
                List.copyOf(correlated), List.copyOf(bridgeCandidates));
        MaterialDiscoveryState.publish(snapshot);

        SilentTinkersMod.LOGGER.info(
                "Material discovery: SG={} materials/{} aliases, TCon={} materials/{} aliases, correlated={}, bridgeCandidates={}",
                silentGear.materials(), silentGear.physicalAliases(),
                tinkers.materials(), tinkers.physicalAliases(),
                correlated.size(), bridgeCandidates.size());

        for (MaterialCorrelationIndex.Candidate candidate : correlated) {
            SilentTinkersMod.LOGGER.info(
                    "[SilentTinkers:CORRELATED] item={} SG={} TCon={}",
                    candidate.physicalItem(),
                    candidate.profiles().get(MaterialProfile.Ecosystem.SILENT_GEAR).materialId(),
                    candidate.profiles().get(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT).materialId());
        }
        for (MaterialCorrelationIndex.Candidate candidate : bridgeCandidates) {
            MaterialProfile profile = candidate.profiles().values().iterator().next();
            SilentTinkersMod.LOGGER.info(
                    "[SilentTinkers:BRIDGE_CANDIDATE] item={} source={} material={}",
                    candidate.physicalItem(), profile.ecosystem(), profile.materialId());
        }

        // Dry-run the generation planner. This deliberately creates no materials
        // yet; it makes the first real modpack test prove our decisions before we
        // allow the generator to mutate either ecosystem.
        List<MaterialGenerationRequest> requests = MaterialGenerationPlanner.fromDiscovery(snapshot);
        long actionable = requests.stream().filter(MaterialGenerationRequest::generatesAnything).count();
        SilentTinkersMod.LOGGER.info(
                "[SilentTinkers:GENERATION_PLAN] total={} actionable={} preserved={}",
                requests.size(), actionable, requests.size() - actionable);
        for (MaterialGenerationRequest request : requests) {
            if (request.generatesAnything()) {
                SilentTinkersMod.LOGGER.info(
                        "[SilentTinkers:GENERATE] item={} action={} source={} target={}",
                        request.physicalItem(), request.action(),
                        request.source().map(Enum::name).orElse("NONE"),
                        request.target().map(Enum::name).orElse("BOTH"));
            }
        }

        return snapshot;
    }

    public record Snapshot(
            MaterialCorrelationIndex index,
            SilentGearDiscoveryBridge.DiscoveryReport silentGear,
            TinkersCorrelationAdapter.DiscoveryReport tinkers,
            List<MaterialCorrelationIndex.Candidate> correlated,
            List<MaterialCorrelationIndex.Candidate> bridgeCandidates) {
        public int correlatedPhysicalItems() { return correlated.size(); }
        public int bridgeCandidateCount() { return bridgeCandidates.size(); }
    }
}
