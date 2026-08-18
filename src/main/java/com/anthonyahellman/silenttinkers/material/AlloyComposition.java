package com.anthonyahellman.silenttinkers.material;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical, compact identity for a Silent Gear alloy as it moves through
 * Tinkers' melting, casting, part assembly, and final tool construction.
 *
 * <p>Equivalent ratios normalize to the same ordered value (for example,
 * 70:20:10 and 7:2:1). This prevents duplicate identities and bounds save data.</p>
 */
public final class AlloyComposition {
    public static final int DATA_VERSION = 1;
    public static final int MAX_INGREDIENTS = 16;

    private static final String VERSION_KEY = "Version";
    private static final String INGREDIENTS_KEY = "Ingredients";

    private final List<MaterialIngredient> ingredients;
    private final long totalUnits;
    private final String fingerprint;

    private AlloyComposition(List<MaterialIngredient> ingredients) {
        this.ingredients = List.copyOf(ingredients);
        this.totalUnits = ingredients.stream().mapToLong(MaterialIngredient::units).sum();
        this.fingerprint = createFingerprint(this.ingredients);
    }

    public static AlloyComposition of(Map<ResourceLocation, Long> rawIngredients) {
        Objects.requireNonNull(rawIngredients, "rawIngredients");
        if (rawIngredients.isEmpty() || rawIngredients.size() > MAX_INGREDIENTS) {
            throw new IllegalArgumentException("An alloy must contain 1 to " + MAX_INGREDIENTS + " materials");
        }

        Map<ResourceLocation, Long> merged = new LinkedHashMap<>();
        rawIngredients.forEach((id, units) -> {
            Objects.requireNonNull(id, "materialId");
            Objects.requireNonNull(units, "units");
            if (units <= 0) {
                throw new IllegalArgumentException("Material units must be positive");
            }
            merged.merge(id, units, Math::addExact);
        });

        long divisor = merged.values().stream().reduce(0L, AlloyComposition::gcd);
        List<MaterialIngredient> normalized = merged.entrySet().stream()
                .map(entry -> new MaterialIngredient(entry.getKey(), entry.getValue() / divisor))
                .sorted(Comparator.comparing(entry -> entry.materialId().toString()))
                .toList();
        return new AlloyComposition(normalized);
    }

    public List<MaterialIngredient> ingredients() {
        return ingredients;
    }

    public long totalUnits() {
        return totalUnits;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public double fraction(ResourceLocation materialId) {
        return ingredients.stream()
                .filter(entry -> entry.materialId().equals(materialId))
                .mapToLong(MaterialIngredient::units)
                .findFirst()
                .orElse(0L) / (double) totalUnits;
    }

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_KEY, DATA_VERSION);
        ListTag list = new ListTag();
        ingredients.forEach(entry -> list.add(entry.save()));
        root.put(INGREDIENTS_KEY, list);
        return root;
    }

    public static AlloyComposition load(CompoundTag root) {
        int version = root.getInt(VERSION_KEY);
        if (version != DATA_VERSION) {
            throw new IllegalArgumentException("Unsupported alloy data version: " + version);
        }

        ListTag list = root.getList(INGREDIENTS_KEY, Tag.TAG_COMPOUND);
        Map<ResourceLocation, Long> ingredients = new LinkedHashMap<>();
        for (Tag value : list) {
            MaterialIngredient ingredient = MaterialIngredient.load((CompoundTag) value);
            ingredients.merge(ingredient.materialId(), ingredient.units(), Math::addExact);
        }
        return of(ingredients);
    }

    private static long gcd(long left, long right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0) {
            long remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }

    private static String createFingerprint(List<MaterialIngredient> ingredients) {
        StringBuilder canonical = new StringBuilder();
        ingredients.forEach(entry -> canonical.append(entry.materialId())
                .append('=')
                .append(entry.units())
                .append(';'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(24);
            for (int index = 0; index < 12; index++) {
                hex.append(String.format("%02x", digest[index]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
