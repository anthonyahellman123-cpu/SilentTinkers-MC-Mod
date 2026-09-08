package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.config.TraitAccess;
import com.anthonyahellman.silenttinkers.config.TraitThresholds;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Pure trait-forwarding decisions; the runtime adapter supplies actual Tinkers modifier entries. */
public final class TraitForwardingPlan {
    private TraitForwardingPlan() {}

    public static <T> List<Decision<T>> create(
            AlloyComposition composition,
            TraitThresholds thresholds,
            Function<ResourceLocation, TraitSourceResolver.Resolution> resolver,
            Function<ResourceLocation, List<T>> traitLookup) {
        Objects.requireNonNull(composition, "composition");
        Objects.requireNonNull(thresholds, "thresholds");
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(traitLookup, "traitLookup");

        List<Decision<T>> decisions = new ArrayList<>();
        for (MaterialIngredient ingredient : composition.ingredients()) {
            double percent = 100.0 * ingredient.units() / composition.totalUnits();
            TraitAccess access = thresholds.accessFor(percent);
            if (access == TraitAccess.NONE) {
                decisions.add(new Decision<>(ingredient.materialId(), percent, access,
                        Optional.empty(), 0, List.of()));
                continue;
            }

            TraitSourceResolver.Resolution resolution = Objects.requireNonNull(
                    resolver.apply(ingredient.materialId()), "resolution");
            if (!resolution.resolved()) {
                decisions.add(new Decision<>(ingredient.materialId(), percent, access,
                        Optional.of(resolution), 0, List.of()));
                continue;
            }

            List<T> available = List.copyOf(Objects.requireNonNull(
                    traitLookup.apply(resolution.materialId().orElseThrow()), "traits"));
            decisions.add(new Decision<>(ingredient.materialId(), percent, access,
                    Optional.of(resolution), available.size(), select(access, available)));
        }
        return List.copyOf(decisions);
    }

    /** Preserves the registry's declared trait order and complete entry values/levels. */
    public static <T> List<T> select(TraitAccess access, List<T> orderedTraits) {
        Objects.requireNonNull(access, "access");
        List<T> traits = List.copyOf(Objects.requireNonNull(orderedTraits, "orderedTraits"));
        int limit = switch (access) {
            case NONE -> 0;
            case PRIMARY -> 1;
            case SECONDARY -> 2;
            case FULL -> traits.size();
        };
        return List.copyOf(traits.subList(0, Math.min(limit, traits.size())));
    }

    public record Decision<T>(ResourceLocation sourceMaterialId,
                              double materialPercent,
                              TraitAccess access,
                              Optional<TraitSourceResolver.Resolution> resolution,
                              int availableTraitCount,
                              List<T> forwardedTraits) {
        public Decision {
            Objects.requireNonNull(sourceMaterialId, "sourceMaterialId");
            Objects.requireNonNull(access, "access");
            resolution = Objects.requireNonNull(resolution, "resolution");
            forwardedTraits = List.copyOf(forwardedTraits);
            if (availableTraitCount < 0 || forwardedTraits.size() > availableTraitCount) {
                throw new IllegalArgumentException("Invalid trait counts");
            }
        }
    }
}
