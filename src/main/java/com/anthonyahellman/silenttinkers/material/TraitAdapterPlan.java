package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Central Silent Gear trait-to-Tinkers adapter catalog and pure planning boundary. */
public final class TraitAdapterPlan {
    public static final ResourceLocation PIXIE_TRAIT = id("silentcompat", "pixie");
    public static final ResourceLocation PIXIE_MODIFIER = id("silenttinkers", "pixie");

    private static final Map<ResourceLocation, Definition> DEFINITIONS = definitions();

    private TraitAdapterPlan() {}

    public static List<Decision> create(Collection<ResourceLocation> sourceTraits, int contributionLevel) {
        Objects.requireNonNull(sourceTraits, "sourceTraits");
        if (contributionLevel < 0 || contributionLevel > 4) {
            throw new IllegalArgumentException("contributionLevel must be from 0 to 4");
        }

        List<Decision> decisions = new ArrayList<>();
        for (ResourceLocation sourceTrait : sourceTraits) {
            Definition definition = DEFINITIONS.get(sourceTrait);
            if (contributionLevel == 0) {
                decisions.add(new Decision(sourceTrait, Status.BELOW_THRESHOLD, contributionLevel,
                        Optional.empty(), "material contribution is below the trait threshold"));
            } else if (definition == null) {
                decisions.add(new Decision(sourceTrait, Status.UNSUPPORTED, contributionLevel,
                        Optional.empty(), "no safe Tinkers adapter is registered"));
            } else {
                decisions.add(new Decision(sourceTrait, Status.SUPPORTED, contributionLevel,
                        Optional.of(definition.targetModifier()), definition.detail()));
            }
        }
        return List.copyOf(decisions);
    }

    private static Map<ResourceLocation, Definition> definitions() {
        Map<ResourceLocation, Definition> result = new LinkedHashMap<>();
        result.put(PIXIE_TRAIT, new Definition(PIXIE_MODIFIER,
                "delegates to Botania's pixie-spawn attribute and damage handler"));
        return Map.copyOf(result);
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    private record Definition(ResourceLocation targetModifier, String detail) {}

    public record Decision(ResourceLocation sourceTrait,
                           Status status,
                           int contributionLevel,
                           Optional<ResourceLocation> targetModifier,
                           String detail) {
        public Decision {
            Objects.requireNonNull(sourceTrait, "sourceTrait");
            Objects.requireNonNull(status, "status");
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
        BELOW_THRESHOLD
    }
}
