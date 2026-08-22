package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.compat.silentgear.SilentGearDiscoveryBridge;
import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersCorrelationAdapter;

import java.util.List;

/** Runs both ecosystem discovery passes into one shared correlation index. */
public final class UnifiedDiscoverySession {
    private UnifiedDiscoverySession() {}

    public static Result run(SilentGearDiscoveryBridge.Provider silentGearProvider) {
        MaterialCorrelationIndex index = new MaterialCorrelationIndex();
        TinkersCorrelationAdapter.DiscoveryReport tinkers = TinkersCorrelationAdapter.populate(index);
        SilentGearDiscoveryBridge.DiscoveryReport silentGear =
                SilentGearDiscoveryBridge.populate(index, silentGearProvider);

        int correlated = 0;
        int bridgeCandidates = 0;
        for (MaterialCorrelationIndex.Candidate candidate : index.all()) {
            if (candidate.correlated()) {
                correlated++;
            } else if (candidate.bridgeCandidate()) {
                bridgeCandidates++;
            }
        }

        return new Result(index, tinkers, silentGear, correlated, bridgeCandidates);
    }

    public record Result(
            MaterialCorrelationIndex index,
            TinkersCorrelationAdapter.DiscoveryReport tinkers,
            SilentGearDiscoveryBridge.DiscoveryReport silentGear,
            int correlatedPhysicalItems,
            int bridgeCandidatePhysicalItems) {

        public List<MaterialCorrelationIndex.Candidate> bridgeCandidates() {
            return index.all().stream()
                    .filter(MaterialCorrelationIndex.Candidate::bridgeCandidate)
                    .toList();
        }

        public List<MaterialCorrelationIndex.Candidate> correlated() {
            return index.all().stream()
                    .filter(MaterialCorrelationIndex.Candidate::correlated)
                    .toList();
        }
    }
}
