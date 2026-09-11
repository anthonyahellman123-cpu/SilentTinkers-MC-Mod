package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Read-only coverage audit for the explicitly prioritized ecosystems. This is
 * reporting/classification over the existing bridge plan, not a second source
 * discovery or compatibility mechanism.
 */
public final class PriorityMaterialAudit {
    private PriorityMaterialAudit() {}

    public static Report evaluate(Collection<MaterialGenerationEvaluation> evaluations) {
        Map<PriorityEcosystem, Map<ResourceLocation, Coverage>> materials = new EnumMap<>(PriorityEcosystem.class);
        for (PriorityEcosystem ecosystem : PriorityEcosystem.values()) {
            materials.put(ecosystem, new TreeMap<>(java.util.Comparator.comparing(ResourceLocation::toString)));
        }

        for (MaterialGenerationEvaluation evaluation : evaluations) {
            ResourceLocation materialId = evaluation.request().sourceMaterialId().orElse(null);
            if (materialId == null) continue;
            Optional<PriorityEcosystem> ecosystem = identify(materialId, evaluation.request().physicalItem());
            ecosystem.ifPresent(value -> materials.get(value).merge(materialId,
                    classify(evaluation.status()), PriorityMaterialAudit::moreRestrictive));
        }

        Map<PriorityEcosystem, Map<Coverage, Integer>> counts = new LinkedHashMap<>();
        materials.forEach((ecosystem, classified) -> {
            Map<Coverage, Integer> values = new EnumMap<>(Coverage.class);
            for (Coverage coverage : Coverage.values()) values.put(coverage, 0);
            classified.values().forEach(coverage -> values.merge(coverage, 1, Integer::sum));
            counts.put(ecosystem, Map.copyOf(values));
        });
        return new Report(Map.copyOf(counts));
    }

    static Optional<PriorityEcosystem> identify(ResourceLocation materialId, ResourceLocation physicalItem) {
        String namespace = materialId.getNamespace();
        String searchable = materialId + "|" + physicalItem;
        if ("iceandfire".equals(namespace) || searchable.contains("dragonsteel")) {
            return Optional.of(PriorityEcosystem.ICE_AND_FIRE_DRAGONSTEEL);
        }
        if ("silentcompat".equals(namespace)) return Optional.of(PriorityEcosystem.SILENT_COMPAT);
        if ("tinkers_advanced".equals(namespace) || "tinkersadvanced".equals(namespace)) {
            return Optional.of(PriorityEcosystem.TINKERS_ADVANCED);
        }
        if ("silentgear".equals(namespace)) return Optional.of(PriorityEcosystem.SILENT_GEAR_CORE);
        return Optional.empty();
    }

    static Coverage classify(MaterialGenerationEvaluation.Status status) {
        return switch (status) {
            case PRESERVED, READY_FOR_TINKERS -> Coverage.SUPPORTED;
            case TINKERS_SOURCE_READY -> Coverage.SUPPORTED_WITH_LIMITATIONS;
            case ROLE_LIMITED -> Coverage.ROLE_LIMITED;
            case BOOTSTRAP_PENDING -> Coverage.DEFERRED;
            case QUARANTINED -> Coverage.QUARANTINED;
        };
    }

    private static Coverage moreRestrictive(Coverage left, Coverage right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    public record Report(Map<PriorityEcosystem, Map<Coverage, Integer>> counts) {
        public int count(PriorityEcosystem ecosystem, Coverage coverage) {
            return counts.getOrDefault(ecosystem, Map.of()).getOrDefault(coverage, 0);
        }

        public int total(PriorityEcosystem ecosystem) {
            return counts.getOrDefault(ecosystem, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    public enum PriorityEcosystem {
        SILENT_GEAR_CORE,
        SILENT_COMPAT,
        TINKERS_ADVANCED,
        ICE_AND_FIRE_DRAGONSTEEL
    }

    /** Ordered from best-supported to most restrictive for duplicate aliases. */
    public enum Coverage {
        SUPPORTED,
        SUPPORTED_WITH_LIMITATIONS,
        ROLE_LIMITED,
        DEFERRED,
        QUARANTINED,
        UNSUPPORTED
    }
}
