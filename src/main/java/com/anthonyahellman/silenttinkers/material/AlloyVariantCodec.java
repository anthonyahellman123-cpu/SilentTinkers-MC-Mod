package com.anthonyahellman.silenttinkers.material;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Encodes a complete composition into Tinkers' material variant string. The
 * format uses only ResourceLocation path characters, so normal Tinkers part and
 * tool NBT preserves it without a world-global runtime material registry.
 */
public final class AlloyVariantCodec {
    private static final String PREFIX = "v1.";
    private static final int MAX_ENCODED_LENGTH = 2048;

    private AlloyVariantCodec() {}

    /** True only for versioned payload variants produced by this codec. */
    public static boolean isEncodedVariant(String encoded) {
        return encoded != null
                && encoded.startsWith(PREFIX)
                && encoded.length() <= MAX_ENCODED_LENGTH;
    }

    public static String encode(AlloyComposition composition) {
        StringBuilder encoded = new StringBuilder(PREFIX);
        for (MaterialIngredient ingredient : composition.ingredients()) {
            if (encoded.length() > PREFIX.length()) {
                encoded.append('-');
            }
            encoded.append(HexFormat.of().formatHex(
                            ingredient.materialId().toString().getBytes(StandardCharsets.UTF_8)))
                    .append('_')
                    .append(Long.toString(ingredient.units(), 36));
        }
        return checked(encoded.toString());
    }

    public static String encode(AlloyComposition composition, int starChargeLevel) {
        String encoded = encode(composition);
        if (starChargeLevel > 0) {
            encoded += ".s" + Integer.toString(starChargeLevel, 36);
        }
        return checked(encoded);
    }

    public static String encode(AlloyComposition composition, int starChargeLevel,
                                Optional<AlloyStatSnapshot> stats) {
        return encode(composition, starChargeLevel, stats, Optional.empty());
    }

    public static String encode(AlloyComposition composition, int starChargeLevel,
                                Optional<AlloyStatSnapshot> stats,
                                Optional<SourceVisualIdentity> visualSource) {
        String encoded = encode(composition, starChargeLevel);
        if (visualSource.isPresent()) {
            encoded += ".v" + HexFormat.of().formatHex(
                    visualSource.get().itemId().toString().getBytes(StandardCharsets.UTF_8));
        }
        if (stats.isPresent()) {
            AlloyStatSnapshot value = stats.get();
            encoded += ".d"
                    + encodeFloat(value.durability()) + '_'
                    + encodeFloat(value.miningSpeed()) + '_'
                    + encodeFloat(value.meleeDamage()) + '_'
                    + encodeFloat(value.attackSpeed()) + '_'
                    + HexFormat.of().formatHex(value.harvestTier().toString().getBytes(StandardCharsets.UTF_8));
        }

        // Tinkers only keeps the MaterialVariantId when a part becomes a tool.
        // Preserve the source item's dynamic visual NBT in that variant when it
        // fits our hard payload budget. Large tags degrade safely to item ID only.
        if (visualSource.isPresent() && visualSource.get().itemTag().isPresent()) {
            CompoundTag sourceTag = visualSource.get().itemTag().orElseThrow();
            String nbtSuffix = ".n" + HexFormat.of().formatHex(
                    sourceTag.toString().getBytes(StandardCharsets.UTF_8));
            if (encoded.length() + nbtSuffix.length() <= MAX_ENCODED_LENGTH) {
                encoded += nbtSuffix;
            }
        }
        return checked(encoded);
    }

    public static AlloyComposition decode(String encoded) {
        validate(encoded);
        String compositionData = stripMetadataSuffix(encoded);
        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        for (String entry : compositionData.substring(PREFIX.length()).split("-")) {
            int separator = entry.lastIndexOf('_');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalArgumentException("Malformed alloy variant entry");
            }
            String materialText;
            try {
                materialText = new String(HexFormat.of().parseHex(entry.substring(0, separator)), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Malformed material encoding", exception);
            }
            ResourceLocation materialId = ResourceLocation.tryParse(materialText);
            if (materialId == null) {
                throw new IllegalArgumentException("Invalid material ID in alloy variant");
            }
            long units;
            try {
                units = Long.parseLong(entry.substring(separator + 1), 36);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid material units in alloy variant", exception);
            }
            ingredients.merge(materialId, units, Math::addExact);
        }
        return AlloyComposition.of(ingredients);
    }

    public static int decodeStarChargeLevel(String encoded) {
        validate(encoded);
        int marker = encoded.indexOf(".s", PREFIX.length());
        if (marker < 0) return 0;
        int end = nextMetadataMarker(encoded, marker + 2);
        String levelText = encoded.substring(marker + 2, end < 0 ? encoded.length() : end);
        try {
            int level = Integer.parseInt(levelText, 36);
            if (level <= 0) throw new IllegalArgumentException("Invalid starcharge level");
            return level;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid starcharge level", exception);
        }
    }

    public static Optional<ResourceLocation> decodeVisualSourceItemId(String encoded) {
        return decodeVisualSource(encoded).map(SourceVisualIdentity::itemId);
    }

    /** Restores as much of the original source stack identity as fit in the variant. */
    public static Optional<SourceVisualIdentity> decodeVisualSource(String encoded) {
        validate(encoded);
        int marker = encoded.indexOf(".v", PREFIX.length());
        if (marker < 0) return Optional.empty();
        int end = nextMetadataMarker(encoded, marker + 2);
        String encodedItem = encoded.substring(marker + 2, end < 0 ? encoded.length() : end);
        try {
            ResourceLocation itemId = ResourceLocation.tryParse(new String(
                    HexFormat.of().parseHex(encodedItem), StandardCharsets.UTF_8));
            if (itemId == null) throw new IllegalArgumentException("Invalid visual source item ID");

            Optional<CompoundTag> itemTag = Optional.empty();
            int nbtMarker = encoded.indexOf(".n", PREFIX.length());
            if (nbtMarker >= 0) {
                int nbtEnd = nextMetadataMarker(encoded, nbtMarker + 2);
                String encodedNbt = encoded.substring(nbtMarker + 2, nbtEnd < 0 ? encoded.length() : nbtEnd);
                String snbt = new String(HexFormat.of().parseHex(encodedNbt), StandardCharsets.UTF_8);
                itemTag = Optional.of(TagParser.parseTag(snbt));
            }
            return Optional.of(new SourceVisualIdentity(itemId, itemTag));
        } catch (IllegalArgumentException | CommandSyntaxException exception) {
            throw new IllegalArgumentException("Malformed visual source identity", exception);
        }
    }

    public static Optional<AlloyStatSnapshot> decodeStats(String encoded) {
        validate(encoded);
        int marker = encoded.indexOf(".d", PREFIX.length());
        if (marker < 0) return Optional.empty();
        int end = nextMetadataMarker(encoded, marker + 2);
        String statData = encoded.substring(marker + 2, end < 0 ? encoded.length() : end);
        String[] fields = statData.split("_", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("Malformed alloy stat snapshot");
        }
        try {
            ResourceLocation tier = ResourceLocation.tryParse(new String(
                    HexFormat.of().parseHex(fields[4]), StandardCharsets.UTF_8));
            if (tier == null) {
                throw new IllegalArgumentException("Invalid harvest tier in alloy stat snapshot");
            }
            return Optional.of(new AlloyStatSnapshot(
                    decodeFloat(fields[0]), decodeFloat(fields[1]),
                    decodeFloat(fields[2]), decodeFloat(fields[3]), tier));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Malformed alloy stat snapshot", exception);
        }
    }

    private static String stripMetadataSuffix(String encoded) {
        int marker = nextMetadataMarker(encoded, PREFIX.length());
        return marker < 0 ? encoded : encoded.substring(0, marker);
    }

    private static int nextMetadataMarker(String encoded, int fromIndex) {
        int next = -1;
        for (String marker : new String[]{".s", ".v", ".d", ".n"}) {
            int candidate = encoded.indexOf(marker, fromIndex);
            if (candidate >= 0 && (next < 0 || candidate < next)) next = candidate;
        }
        return next;
    }

    private static String checked(String encoded) {
        if (encoded.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Encoded alloy exceeds " + MAX_ENCODED_LENGTH + " characters");
        }
        return encoded;
    }

    private static void validate(String encoded) {
        if (!isEncodedVariant(encoded)) {
            throw new IllegalArgumentException("Unsupported alloy variant");
        }
    }

    private static String encodeFloat(float value) {
        return Integer.toUnsignedString(Float.floatToIntBits(value), 36);
    }

    private static float decodeFloat(String value) {
        long bits = Long.parseUnsignedLong(value, 36);
        if (bits > 0xFFFF_FFFFL) {
            throw new NumberFormatException("Float bits exceed 32 bits");
        }
        return Float.intBitsToFloat((int) bits);
    }
}
