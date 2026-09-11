package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/** Central Silent Gear trait-to-Tinkers adapter catalog and pure planning boundary. */
public final class TraitAdapterPlan {
    public static final ResourceLocation PIXIE_TRAIT = id("silentcompat", "pixie");
    public static final ResourceLocation PIXIE_MODIFIER = id("silenttinkers", "pixie");
    public static final ResourceLocation MAGNETIC_TRAIT = id("silentgear", "magnetic");
    public static final ResourceLocation MAGNETIC_MODIFIER = id("tconstruct", "magnetic");
    public static final ResourceLocation WITHER_SKULL_TRAIT = id("silentcompat", "wither_skull");

    private static final Map<ResourceLocation, Definition> DEFINITIONS = definitions();

    private TraitAdapterPlan() {}

    public static List<Decision> create(Collection<ResourceLocation> sourceTraits, int contributionLevel) {
        return create(sourceTraits, contributionLevel, ignored -> true);
    }

    public static List<Decision> create(Collection<ResourceLocation> sourceTraits, int contributionLevel,
                                        Predicate<ResourceLocation> targetAvailable) {
        Objects.requireNonNull(sourceTraits, "sourceTraits");
        Objects.requireNonNull(targetAvailable, "targetAvailable");
        if (contributionLevel < 0 || contributionLevel > 4) {
            throw new IllegalArgumentException("contributionLevel must be from 0 to 4");
        }

        List<Decision> decisions = new ArrayList<>();
        for (ResourceLocation sourceTrait : new java.util.LinkedHashSet<>(sourceTraits)) {
            Objects.requireNonNull(sourceTrait, "sourceTrait");
            Definition definition = DEFINITIONS.get(sourceTrait);
            if (contributionLevel == 0) {
                decisions.add(new Decision(sourceTrait, Status.BELOW_THRESHOLD, contributionLevel,
                        Kind.UNCLASSIFIED, Optional.empty(), "material contribution is below the trait threshold"));
            } else if (definition == null) {
                decisions.add(new Decision(sourceTrait, Status.UNSUPPORTED, contributionLevel,
                        Kind.UNCLASSIFIED, Optional.empty(), "no safe Tinkers mapping or behavioral adapter is registered"));
            } else if (definition.targetModifier().isEmpty()) {
                decisions.add(new Decision(sourceTrait, Status.UNSUPPORTED, contributionLevel,
                        definition.kind(), Optional.empty(), definition.detail()));
            } else if (!targetAvailable.test(definition.targetModifier().orElseThrow())) {
                decisions.add(new Decision(sourceTrait, Status.TARGET_UNAVAILABLE, contributionLevel,
                        definition.kind(), Optional.empty(), "mapped modifier is not registered: "
                        + definition.targetModifier().orElseThrow()));
            } else {
                decisions.add(new Decision(sourceTrait, Status.SUPPORTED, contributionLevel,
                        definition.kind(), definition.targetModifier(), definition.detail()));
            }
        }
        return List.copyOf(decisions);
    }

    /** Collapses equivalent adapter targets deterministically without flattening native material entries. */
    public static Map<ResourceLocation, Integer> applications(Collection<Decision> decisions,
                                                               Collection<ResourceLocation> nativeTargets) {
        Objects.requireNonNull(decisions, "decisions");
        Objects.requireNonNull(nativeTargets, "nativeTargets");
        java.util.Set<ResourceLocation> excluded = java.util.Set.copyOf(nativeTargets);
        Map<ResourceLocation, Integer> result = new java.util.TreeMap<>(
                java.util.Comparator.comparing(ResourceLocation::toString));
        for (Decision decision : decisions) {
            if (decision.status() != Status.SUPPORTED) continue;
            ResourceLocation target = decision.targetModifier().orElseThrow();
            if (!excluded.contains(target)) result.merge(target, decision.contributionLevel(), Math::max);
        }
        return Map.copyOf(result);
    }

    private static Map<ResourceLocation, Definition> definitions() {
        Map<ResourceLocation, Definition> result = new LinkedHashMap<>();
        result.put(MAGNETIC_TRAIT, new Definition(Kind.DIRECT, Optional.of(MAGNETIC_MODIFIER),
                "explicit magnetic mapping; Tinkers activation semantics apply"));
        result.put(PIXIE_TRAIT, new Definition(Kind.BEHAVIORAL, Optional.of(PIXIE_MODIFIER),
                "delegates to Botania's pixie-spawn attribute and damage handler"));
        result.put(WITHER_SKULL_TRAIT, new Definition(Kind.BEHAVIORAL, Optional.empty(),
                "active projectile-on-swing behavior requires a server-authoritative adapter"));
        return Map.copyOf(result);
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    private record Definition(Kind kind, Optional<ResourceLocation> targetModifier, String detail) {}

    public record Decision(ResourceLocation sourceTrait,
                           Status status,
                           int contributionLevel,
                           Kind kind,
                           Optional<ResourceLocation> targetModifier,
                           String detail) {
        public Decision {
            Objects.requireNonNull(sourceTrait, "sourceTrait");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(kind, "kind");
            targetModifier = Objects.requireNonNull(targetModifier, "targetModifier");
            Objects.requireNonNull(detail, "detail");
            if ((status == Status.SUPPORTED) != targetModifier.isPresent()) {
                throw new IllegalArgumentException("Only supported adapters have a target modifier");
            }
        }
    }

    public enum Status {
        SUPPORTED,
        UNSUPPORTED,
        TARGET_UNAVAILABLE,
        BELOW_THRESHOLD
    }

    public enum Kind {
        DIRECT,
        BEHAVIORAL,
        UNCLASSIFIED
    }
}
