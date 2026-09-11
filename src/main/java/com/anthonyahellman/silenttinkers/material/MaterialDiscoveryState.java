package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the latest completed read-only material discovery snapshot and the
 * immutable runtime lookup derived from it.
 *
 * <p>Runtime recipe checks happen frequently, so they must not linearly scan
 * hundreds of evaluations every smeltery tick. Publishing a snapshot builds an
 * O(1) canonical-item lookup once, then readers share it until the next reload.</p>
 */
public final class MaterialDiscoveryState {
    private static final AtomicReference<UnifiedMaterialDiscovery.Snapshot> CURRENT =
            new AtomicReference<>();
    private static final AtomicReference<Map<ResourceLocation, MaterialGenerationEvaluation>> READY_FOR_TINKERS =
            new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<ResourceLocation, List<ResourceLocation>>> SILENT_GEAR_TRAITS =
            new AtomicReference<>(Map.of());

    private MaterialDiscoveryState() {}

    public static synchronized void publish(UnifiedMaterialDiscovery.Snapshot snapshot) {
        Map<ResourceLocation, MaterialGenerationEvaluation> ready = new LinkedHashMap<>();
        Map<ResourceLocation, LinkedHashSet<ResourceLocation>> silentGearTraits = new LinkedHashMap<>();
        Set<ResourceLocation> duplicates = new LinkedHashSet<>();

        for (MaterialCorrelationIndex.Candidate candidate : snapshot.index().all()) {
            MaterialProfile profile = candidate.profiles().get(MaterialProfile.Ecosystem.SILENT_GEAR);
            if (profile != null) {
                silentGearTraits.computeIfAbsent(profile.materialId(), ignored -> new LinkedHashSet<>())
                        .addAll(profile.traits());
            }
        }

        for (MaterialGenerationEvaluation evaluation : snapshot.evaluations()) {
            if (!evaluation.readyForMutation()) continue;
            ResourceLocation item = evaluation.request().physicalItem();

            // The first runtime bridge intentionally treats one source item as
            // one ingot-equivalent. Do not expose blocks, nuggets, planks, panes,
            // or arbitrary components until unit-aware conversion is available.
            if (!DynamicBridgeItemPolicy.isOneUnitMaterial(item)) continue;

            if (ready.putIfAbsent(item, evaluation) != null) duplicates.add(item);
        }

        // Duplicate canonical runtime keys are unsafe because a recipe could not
        // deterministically know which material identity to forge. Quarantine the
        // duplicated keys while leaving every unambiguous bridge available.
        for (ResourceLocation duplicate : duplicates) {
            ready.remove(duplicate);
            SilentTinkersMod.LOGGER.warn(
                    "[SilentTinkers:RUNTIME_KEY_AMBIGUOUS] item={} -- excluded from dynamic bridge recipes",
                    duplicate);
        }

        READY_FOR_TINKERS.set(Map.copyOf(ready));
        Map<ResourceLocation, List<ResourceLocation>> immutableTraits = new LinkedHashMap<>();
        silentGearTraits.forEach((material, traits) -> immutableTraits.put(material, List.copyOf(traits)));
        SILENT_GEAR_TRAITS.set(Map.copyOf(immutableTraits));
        CURRENT.set(snapshot);
    }

    public static Optional<UnifiedMaterialDiscovery.Snapshot> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Optional<MaterialGenerationEvaluation> readyForTinkers(ResourceLocation physicalItem) {
        return Optional.ofNullable(READY_FOR_TINKERS.get().get(physicalItem));
    }

    public static int readyForTinkersCount() {
        return READY_FOR_TINKERS.get().size();
    }

    public static List<ResourceLocation> silentGearTraits(ResourceLocation materialId) {
        return SILENT_GEAR_TRAITS.get().getOrDefault(materialId, List.of());
    }

    public static synchronized void clear() {
        READY_FOR_TINKERS.set(Map.of());
        SILENT_GEAR_TRAITS.set(Map.of());
        CURRENT.set(null);
    }
}
