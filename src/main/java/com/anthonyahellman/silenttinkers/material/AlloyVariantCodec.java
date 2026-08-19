package com.anthonyahellman.silenttinkers.material;

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
        if (encoded.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Encoded alloy exceeds " + MAX_ENCODED_LENGTH + " characters");
        }
        return encoded.toString();
    }

    public static String encode(AlloyComposition composition, int starChargeLevel) {
        String encoded = encode(composition);
        if (starChargeLevel <= 0) {
            return encoded;
        }
        String charged = encoded + ".s" + Integer.toString(starChargeLevel, 36);
        if (charged.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Encoded alloy exceeds " + MAX_ENCODED_LENGTH + " characters");
        }
        return charged;
    }

    public static String encode(AlloyComposition composition, int starChargeLevel,
                                Optional<AlloyStatSnapshot> stats) {
        String encoded = encode(composition, starChargeLevel);
        if (stats.isEmpty()) {
            return encoded;
        }
        AlloyStatSnapshot value = stats.get();
        String statData = ".d"
                + encodeFloat(value.durability()) + '_'
                + encodeFloat(value.miningSpeed()) + '_'
                + encodeFloat(value.meleeDamage()) + '_'
                + encodeFloat(value.attackSpeed()) + '_'
                + HexFormat.of().formatHex(value.harvestTier().toString().getBytes(StandardCharsets.UTF_8));
        if (encoded.length() + statData.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Encoded alloy exceeds " + MAX_ENCODED_LENGTH + " characters");
        }
        return encoded + statData;
    }

    public static AlloyComposition decode(String encoded) {
        if (!encoded.startsWith(PREFIX) || encoded.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Unsupported alloy variant");
        }
        String compositionData = stripChargeSuffix(encoded);
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
        if (!encoded.startsWith(PREFIX) || encoded.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Unsupported alloy variant");
        }
        int marker = encoded.lastIndexOf(".s");
        if (marker < 0) {
            return 0;
        }
        int end = encoded.indexOf('.', marker + 2);
        String levelText = encoded.substring(marker + 2, end < 0 ? encoded.length() : end);
        try {
            int level = Integer.parseInt(levelText, 36);
            if (level <= 0) {
                throw new IllegalArgumentException("Invalid starcharge level");
            }
            return level;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid starcharge level", exception);
        }
    }

    public static Optional<AlloyStatSnapshot> decodeStats(String encoded) {
        if (!encoded.startsWith(PREFIX) || encoded.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Unsupported alloy variant");
        }
        int marker = encoded.lastIndexOf(".d");
        if (marker < 0) {
            return Optional.empty();
        }
        String[] fields = encoded.substring(marker + 2).split("_", -1);
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

    private static String stripChargeSuffix(String encoded) {
        int charge = encoded.indexOf(".s", PREFIX.length());
        int stats = encoded.indexOf(".d", PREFIX.length());
        int marker;
        if (charge < 0) {
            marker = stats;
        } else if (stats < 0) {
            marker = charge;
        } else {
            marker = Math.min(charge, stats);
        }
        return marker < 0 ? encoded : encoded.substring(0, marker);
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
