package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Pure, conservative correlation from a composite source ID to a loaded Tinkers material ID. */
public final class TraitSourceResolver {
    private TraitSourceResolver() {}

    public static Resolution resolve(ResourceLocation sourceId,
                                     Collection<ResourceLocation> registeredMaterialIds) {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(registeredMaterialIds, "registeredMaterialIds");

        List<ResourceLocation> registered = registeredMaterialIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        if (registered.contains(sourceId)) {
            return Resolution.resolved(Status.EXACT, sourceId);
        }

        ResourceLocation tconstruct = new ResourceLocation("tconstruct", sourceId.getPath());
        if (registered.contains(tconstruct)) {
            return Resolution.resolved(Status.TCONSTRUCT_PATH, tconstruct);
        }

        List<ResourceLocation> samePath = registered.stream()
                .filter(id -> id.getPath().equals(sourceId.getPath()))
                .toList();
        if (samePath.size() == 1) {
            return Resolution.resolved(Status.UNIQUE_PATH, samePath.get(0));
        }
        if (samePath.size() > 1) {
            return new Resolution(Status.AMBIGUOUS, Optional.empty(), samePath);
        }
        return new Resolution(Status.UNRESOLVED, Optional.empty(), List.of());
    }

    public enum Status {
        EXACT,
        TCONSTRUCT_PATH,
        UNIQUE_PATH,
        AMBIGUOUS,
        UNRESOLVED
    }

    public record Resolution(Status status,
                             Optional<ResourceLocation> materialId,
                             List<ResourceLocation> candidates) {
        public Resolution {
            Objects.requireNonNull(status, "status");
            materialId = Objects.requireNonNull(materialId, "materialId");
            candidates = List.copyOf(candidates);
            boolean resolved = status == Status.EXACT
                    || status == Status.TCONSTRUCT_PATH
                    || status == Status.UNIQUE_PATH;
            if (resolved != materialId.isPresent()) {
                throw new IllegalArgumentException("Resolution status and material ID disagree");
            }
        }

        private static Resolution resolved(Status status, ResourceLocation materialId) {
            return new Resolution(status, Optional.of(materialId), List.of(materialId));
        }

        public boolean resolved() {
            return materialId.isPresent();
        }
    }
}
