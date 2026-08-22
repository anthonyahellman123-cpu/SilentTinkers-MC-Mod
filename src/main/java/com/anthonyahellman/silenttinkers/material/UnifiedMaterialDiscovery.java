package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearApiProvider;
import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearDiscoveryBridge;
import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersCorrelationAdapter;

/**
 * Builds one correlation snapshot from both loaded material ecosystems.
 *
 * <p>The snapshot is deliberately rebuilt as a unit so aliases from a previous
 * datapack state cannot leak into a later discovery pass.</p>
 */
public final class UnifiedMaterialDiscovery {
    private UnifiedMaterialDiscovery() {}

    public static Snapshot discover() {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();

        SilentGearDiscoveryBridge.DiscoveryReport silentGear =
                SilentGearDiscoveryBridge.populate(index, new SilentGearApiProvider());
        TinkersCorrelationAdapter.DiscoveryReport tinkers =
                TinkersCorrelationAdapter.populate(index);

        int correlated = 0;
        int bridgeCandidates = 0;
        for (MaterialCorrelationIndex.Candidate candidate : index.all()) {
            if (candidate.correlated()) {
                correlated++;
            } else if (candidate.bridgeCandidate()) {
                bridgeCandidates++;
            }
        }

        Snapshot snapshot = new Snapshot(index, silentGear, tinkers, correlated, bridgeCandidates);
        SilentTinkersMod.LOGGER.info(
                "Material discovery: SG={} materials/{} aliases, TCon={} materials/{} aliases, correlated={}, bridgeCandidates={}",
                silentGear.materials(), silentGear.physicalAliases(),
                tinkers.materials(), tinkers.physicalAliases(),
                correlated, bridgeCandidates);
        return snapshot;
    }

    public record Snapshot(
            MaterialCorrelationIndex index,
            SilentGearDiscoveryBridge.DiscoveryReport silentGear,
            TinkersCorrelationAdapter.DiscoveryReport tinkers,
            int correlatedPhysicalItems,
            int bridgeCandidates) {}
}
