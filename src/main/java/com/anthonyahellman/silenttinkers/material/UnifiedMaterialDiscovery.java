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

        Snapshot snapshot = new Snapshot(index, silentGear, tinkers, List.copyOf(correlated), List.copyOf(bridgeCandidates));
        MaterialDiscoveryState.publish(snapshot);

        SilentTinkersMod.LOGGER.info("Material discovery: SG={} materials/{} aliases, TCon={} materials/{} aliases, correlated={}, bridgeCandidates={}, ambiguous={}",
                silentGear.materials(), silentGear.physicalAliases(), tinkers.materials(), tinkers.physicalAliases(),
                correlated.size(), bridgeCandidates.size(), ambiguous.size());

        for (MaterialCorrelationIndex.Candidate candidate : ambiguous) {
            SilentTinkersMod.LOGGER.warn("[SilentTinkers:AMBIGUOUS] item={} claims={} -- quarantined from generation",
                    candidate.physicalItem(), candidate.claimIds());
        }
        for (MaterialCorrelationIndex.Candidate candidate : correlated) {
            SilentTinkersMod.LOGGER.info("[SilentTinkers:CORRELATED] item={} SG={} TCon={}", candidate.physicalItem(),
                    candidate.profiles().get(MaterialProfile.Ecosystem.SILENT_GEAR).materialId(),
                    candidate.profiles().get(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT).materialId());
        }
        for (MaterialCorrelationIndex.Candidate candidate : bridgeCandidates) {
            MaterialProfile profile = candidate.profiles().values().iterator().next();
            SilentTinkersMod.LOGGER.info("[SilentTinkers:BRIDGE_CANDIDATE] item={} source={} material={}",
                    candidate.physicalItem(), profile.ecosystem(), profile.materialId());
        }

        List<MaterialGenerationRequest> requests = MaterialGenerationPlanner.fromDiscovery(snapshot);
        long actionable = requests.stream().filter(MaterialGenerationRequest::generatesAnything).count();
        SilentTinkersMod.LOGGER.info("[SilentTinkers:GENERATION_PLAN] total={} actionable={} preserved={} quarantined={}",
                requests.size(), actionable, requests.size() - actionable, ambiguous.size());

        int translated = 0;
        int tinkersNativeReady = 0;
        int translationFailed = 0;
        for (MaterialGenerationRequest request : requests) {
            if (!request.generatesAnything()) continue;

            SilentTinkersMod.LOGGER.info("[SilentTinkers:GENERATE] item={} action={} source={} sourceMaterial={} target={}",
                    request.physicalItem(), request.action(), request.source().map(Enum::name).orElse("NONE"),
                    request.sourceMaterialId().map(Object::toString).orElse("NONE"),
                    request.target().map(Enum::name).orElse("BOTH"));

            if (request.action() != MaterialBridgePlan.Action.BRIDGE) continue;
            if (request.source().orElse(null) == MaterialProfile.Ecosystem.TINKERS_CONSTRUCT) {
                var nativeStats = MaterialStatTranslator.readNativeTinkers(request);
                if (nativeStats.isPresent()) {
                    var value = nativeStats.get();
                    tinkersNativeReady++;
                    SilentTinkersMod.LOGGER.info(
                            "[SilentTinkers:TINKERS_NATIVE] item={} sourceMaterial={} headDurability={} miningSpeed={} meleeAttack={} tier={} handleDurability={} handleMiningSpeed={} handleAttackSpeed={} handleDamage={}",
                            request.physicalItem(), request.sourceMaterialId().map(Object::toString).orElse("NONE"),
                            value.headDurability(), value.headMiningSpeed(), value.headMeleeAttack(), value.harvestTier(),
                            value.handleDurabilityModifier(), value.handleMiningSpeedModifier(),
                            value.handleAttackSpeedModifier(), value.handleDamageModifier());
                } else {
                    translationFailed++;
                    SilentTinkersMod.LOGGER.warn(
                            "[SilentTinkers:TINKERS_NATIVE_FAILED] item={} sourceMaterial={} -- quarantined from generation",
                            request.physicalItem(), request.sourceMaterialId().map(Object::toString).orElse("NONE"));
                }
                continue;
            }

            var stats = MaterialStatTranslator.translate(request);
            if (stats.isPresent()) {
                TranslatedMaterialStats value = stats.get();
                translated++;
                SilentTinkersMod.LOGGER.info(
                        "[SilentTinkers:TRANSLATED] item={} sourceMaterial={} durability={} miningSpeed={} meleeDamage={} attackSpeed={} tier={}",
                        request.physicalItem(), request.sourceMaterialId().map(Object::toString).orElse("NONE"),
                        value.durability(), value.miningSpeed(), value.meleeDamage(), value.attackSpeed(), value.harvestTier());
            } else {
                translationFailed++;
                SilentTinkersMod.LOGGER.warn(
                        "[SilentTinkers:TRANSLATION_FAILED] item={} source={} sourceMaterial={} -- quarantined from generation",
                        request.physicalItem(), request.source().map(Enum::name).orElse("NONE"),
                        request.sourceMaterialId().map(Object::toString).orElse("NONE"));
            }
        }

        SilentTinkersMod.LOGGER.info(
                "[SilentTinkers:TRANSLATION_PLAN] translated={} tinkersNativeReady={} failed={}",
                translated, tinkersNativeReady, translationFailed);
        return snapshot;
    }

    public record Snapshot(MaterialCorrelationIndex index,
            SilentGearDiscoveryBridge.DiscoveryReport silentGear,
            TinkersCorrelationAdapter.DiscoveryReport tinkers,
            List<MaterialCorrelationIndex.Candidate> correlated,
            List<MaterialCorrelationIndex.Candidate> bridgeCandidates) {
        public int correlatedPhysicalItems() { return correlated.size(); }
        public int bridgeCandidateCount() { return bridgeCandidates.size(); }
    }
}
