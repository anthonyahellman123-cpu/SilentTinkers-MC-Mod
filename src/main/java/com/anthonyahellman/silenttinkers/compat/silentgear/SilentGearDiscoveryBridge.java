package com.anthonyahellman.silenttinkers.compat.silentgear;

import com.anthonyahellman.silenttinkers.material.MaterialCorrelationIndex;
import com.anthonyahellman.silenttinkers.material.MaterialProfile;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Stable boundary between SilentTinkers' unified catalog and a Silent Gear
 * catalog provider.
 *
 * <p>The core bridge deliberately does not compile against Silent Gear classes.
 * A runtime/API-specific provider can translate SG's material registry into
 * {@link Entry} values, while the correlation machinery remains independent of
 * Silent Gear implementation details.</p>
 */
public final class SilentGearDiscoveryBridge {
    private SilentGearDiscoveryBridge() {}

    public static DiscoveryReport populate(MaterialCorrelationIndex index, Provider provider) {
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(provider, "provider");

        int materials = 0;
        int aliases = 0;
        int unresolved = 0;

        for (Entry entry : provider.discover()) {
            materials++;
            if (entry.physicalItems().isEmpty()) {
                unresolved++;
                continue;
            }

            MaterialProfile profile = new MaterialProfile(
                    MaterialProfile.Ecosystem.SILENT_GEAR,
                    entry.materialId(),
                    entry.traitIds());
            index.acceptAll(entry.physicalItems(), profile);
            aliases += entry.physicalItems().size();
        }

        return new DiscoveryReport(materials, aliases, unresolved);
    }

    /** Implemented by the SG-specific runtime adapter once its API is available. */
    @FunctionalInterface
    public interface Provider {
        Collection<Entry> discover();
    }

    public record Entry(
            ResourceLocation materialId,
            Collection<ResourceLocation> physicalItems,
            List<ResourceLocation> traitIds) {
        public Entry {
            Objects.requireNonNull(materialId, "materialId");
            physicalItems = List.copyOf(physicalItems);
            traitIds = List.copyOf(traitIds);
        }
    }

    public record DiscoveryReport(int materials, int physicalAliases, int unresolvedMaterials) {}
}
