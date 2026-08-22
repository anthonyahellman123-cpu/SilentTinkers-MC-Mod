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
        SilentGearDiscoveryBridge.DiscoveryReport silentGear = SilentGearDiscoveryBridge.populate(index, new SilentGearApiProvider());
        TinkersCorrelationAdapter.DiscoveryReport tinkers = TinkersCorrelationAdapter.populate(index);

        List<MaterialCorrelationIndex.Candidate> correlated = new ArrayList<>();
        List<MaterialCorrelationIndex.Candidate> bridgeCandidates = new ArrayList<>();
        List<MaterialCorrelationIndex.Candidate> ambiguous = new ArrayList<>();
        for (MaterialCorrelationIndex.Candidate candidate : index.all()) {
            if (candidate.ambiguous()) ambiguous.add(candidate);
            else if (candidate.correlated()) correlated.add(candidate);
            else if (candidate.bridgeCandidate()) bridgeCandidates.add(candidate);
        }

        Comparator<MaterialCorrelationIndex.Candidate> byItem = Comparator.comparing(candidate -> candidate.physicalItem().toString());
        correlated.sort(byItem);
        bridgeCandidates.sort(byItem);
        ambiguous.sort(byItem);

        Snapshot planningSnapshot = new Snapshot(index, silentGear, tinkers,
                List.copyOf(correlated), List.copyOf(bridgeCandidates), List.of());

        SilentTinkersMod.LOGGER.info("Material discovery: SG={} materials/{} aliases, TCon={} materials/{} aliases, correlated={}, bridgeCandidates={}, ambiguous={}",
                silentGear.materials(), silentGear.physicalAliases(), tinkers.materials(), tinkers.physicalAliases(),
                correlated.size(), bridgeCandidates.size(), ambiguous.size());

        for (MaterialCorrelationIndex.Candidate candidate : ambiguous) {
            SilentTinkersMod.LOGGER.warn("[SilentTinkers:AMBIGUOUS] item={} claims={} -- quarantined from generation",
                    candidate.physicalItem(), candidate.claimIds());
        }

        List<MaterialGenerationRequest> requests = MaterialGenerationPlanner.fromDiscovery(planningSnapshot);
        long actionable = requests.stream().filter(MaterialGenerationRequest::generatesAnything).count();
        SilentTinkersMod.LOGGER.info("[SilentTinkers:GENERATION_PLAN] total={} actionable={} preserved={} aliasQuarantined={}",
                requests.size(), actionable, requests.size() - actionable, ambiguous.size());

        int readyForTinkers = 0;
        int tinkersSourceReady = 0;
        int bootstrapPending = 0;
        int requestQuarantined = 0;
        List<MaterialGenerationEvaluation> evaluations = new ArrayList<>(requests.size());

        for (MaterialGenerationRequest request : requests) {
            MaterialGenerationEvaluation evaluation = MaterialGenerationEvaluator.evaluate(request);
            evaluations.add(evaluation);
            if (evaluation.status() == MaterialGenerationEvaluation.Status.PRESERVED) continue;

            SilentTinkersMod.LOGGER.info(
                    "[SilentTinkers:EVALUATE] item={} action={} source={} sourceMaterial={} target={} status={} detail={}",
                    request.physicalItem(), request.action(), request.source().map(Enum::name).orElse("NONE"),
                    request.sourceMaterialId().map(Object::toString).orElse("NONE"),
                    request.target().map(Enum::name).orElse("BOTH"), evaluation.status(), evaluation.detail());

            switch (evaluation.status()) {
                case READY_FOR_TINKERS -> {
                    readyForTinkers++;
                    TranslatedMaterialStats value = evaluation.translatedStats().orElseThrow();
                    SilentTinkersMod.LOGGER.info(
                            "[SilentTinkers:TRANSLATED] item={} durability={} miningSpeed={} meleeDamage={} attackSpeed={} tier={}",
                            request.physicalItem(), value.durability(), value.miningSpeed(), value.meleeDamage(),
                            value.attackSpeed(), value.harvestTier());
                }
                case TINKERS_SOURCE_READY -> {
                    tinkersSourceReady++;
                    var value = evaluation.tinkersSourceStats().orElseThrow();
                    SilentTinkersMod.LOGGER.info(
                            "[SilentTinkers:TINKERS_NATIVE] item={} headDurability={} miningSpeed={} meleeAttack={} tier={} handleDurability={} handleMiningSpeed={} handleAttackSpeed={} handleDamage={}",
                            request.physicalItem(), value.headDurability(), value.headMiningSpeed(), value.headMeleeAttack(),
                            value.harvestTier(), value.handleDurabilityModifier(), value.handleMiningSpeedModifier(),
                            value.handleAttackSpeedModifier(), value.handleDamageModifier());
                }
                case BOOTSTRAP_PENDING -> bootstrapPending++;
                case QUARANTINED -> {
                    requestQuarantined++;
                    SilentTinkersMod.LOGGER.warn("[SilentTinkers:REQUEST_QUARANTINED] item={} reason={}",
                            request.physicalItem(), evaluation.detail());
                }
                case PRESERVED -> { }
            }
        }

        SilentTinkersMod.LOGGER.info(
                "[SilentTinkers:EVALUATION_PLAN] readyForTinkers={} tinkersSourceReady={} bootstrapPending={} requestQuarantined={} aliasQuarantined={}",
                readyForTinkers, tinkersSourceReady, bootstrapPending, requestQuarantined, ambiguous.size());

        Snapshot completedSnapshot = new Snapshot(index, silentGear, tinkers,
                List.copyOf(correlated), List.copyOf(bridgeCandidates), List.copyOf(evaluations));
        MaterialDiscoveryState.publish(completedSnapshot);
        return completedSnapshot;
    }

    public record Snapshot(MaterialCorrelationIndex index,
            SilentGearDiscoveryBridge.DiscoveryReport silentGear,
            TinkersCorrelationAdapter.DiscoveryReport tinkers,
            List<MaterialCorrelationIndex.Candidate> correlated,
            List<MaterialCorrelationIndex.Candidate> bridgeCandidates,
            List<MaterialGenerationEvaluation> evaluations) {
        public int correlatedPhysicalItems() { return correlated.size(); }
        public int bridgeCandidateCount() { return bridgeCandidates.size(); }
        public long readyForMutationCount() {
            return evaluations.stream().filter(MaterialGenerationEvaluation::readyForMutation).count();
        }
    }
}
