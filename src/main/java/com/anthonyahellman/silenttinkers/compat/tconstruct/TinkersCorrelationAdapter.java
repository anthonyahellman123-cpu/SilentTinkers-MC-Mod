package com.anthonyahellman.silenttinkers.compat.tconstruct;

import com.anthonyahellman.silenttinkers.material.MaterialCorrelationIndex;

/** Feeds Tinkers' read-only discovery results into the shared correlation index. */
public final class TinkersCorrelationAdapter {
    private TinkersCorrelationAdapter() {}

    public static DiscoveryReport populate(MaterialCorrelationIndex index) {
        int materials = 0;
        int aliases = 0;
        int unresolved = 0;

        for (TinkersMaterialDiscovery.DiscoveredMaterial discovered : TinkersMaterialDiscovery.discover()) {
            materials++;
            if (discovered.physicalItems().isEmpty()) {
                unresolved++;
                continue;
            }
            index.acceptAll(discovered.physicalItems(), discovered.profile());
            aliases += discovered.physicalItems().size();
        }

        return new DiscoveryReport(materials, aliases, unresolved);
    }

    public record DiscoveryReport(int materials, int physicalAliases, int unresolvedMaterials) {}
}
