package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic identity contract for future SilentTinkers-generated profiles.
 *
 * <p>No reverse generation is performed here. The encoded owner/source identity
 * lets a later discovery pass recognize its own output and correlate it back to
 * the same source instead of treating it as a new foreign material.</p>
 */
public final class GeneratedMaterialOwnership {
    private static final String NAMESPACE = "silenttinkers";
    private static final String PREFIX = "generated/";

    private GeneratedMaterialOwnership() {}

    public static ResourceLocation idFor(MaterialProfile.Ecosystem target,
                                         ResourceLocation sourceMaterialId) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(sourceMaterialId, "sourceMaterialId");
        String targetToken = target.name().toLowerCase(Locale.ROOT);
        return new ResourceLocation(NAMESPACE, PREFIX + targetToken + "/"
                + sourceMaterialId.getNamespace() + "/" + sourceMaterialId.getPath());
    }

    public static boolean isOwned(ResourceLocation materialId) {
        return describe(materialId).isPresent();
    }

    public static Optional<Ownership> describe(ResourceLocation materialId) {
        if (materialId == null
                || !NAMESPACE.equals(materialId.getNamespace())
                || !materialId.getPath().startsWith(PREFIX)) {
            return Optional.empty();
        }

        String encoded = materialId.getPath().substring(PREFIX.length());
        String[] fields = encoded.split("/", 3);
        if (fields.length != 3) return Optional.empty();

        MaterialProfile.Ecosystem target;
        try {
            target = MaterialProfile.Ecosystem.valueOf(fields[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        ResourceLocation source = ResourceLocation.tryParse(fields[1] + ":" + fields[2]);
        if (source == null || !idFor(target, source).equals(materialId)) return Optional.empty();
        return Optional.of(new Ownership(target, source));
    }

    public record Ownership(MaterialProfile.Ecosystem target,
                            ResourceLocation sourceMaterialId) {
        public Ownership {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(sourceMaterialId, "sourceMaterialId");
        }
    }
}
