package com.anthonyahellman.silenttinkers.client;

import com.anthonyahellman.silenttinkers.material.AlloyComposition;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
import com.anthonyahellman.silenttinkers.material.SourceVisualIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves a ratio-weighted visual color from Silent Gear's real material display items. */
public final class AlloyVisualColorResolver {
    private static final String GEAR_API = "net.silentchaos512.gear.api.GearApi";
    private static final String MATERIAL = "net.silentchaos512.gear.api.material.IMaterial";
    private static final String PART_TYPE = "net.silentchaos512.gear.api.part.PartType";
    private static final Map<String, Integer> CACHE = new ConcurrentHashMap<>();

    private AlloyVisualColorResolver() {}

    public static int resolve(AlloyComposition composition, Optional<SourceVisualIdentity> fallback) {
        return CACHE.computeIfAbsent(composition.fingerprint(), ignored -> mix(composition, fallback));
    }

    private static int mix(AlloyComposition composition, Optional<SourceVisualIdentity> fallback) {
        double red = 0;
        double green = 0;
        double blue = 0;
        long sampledUnits = 0;

        for (MaterialIngredient ingredient : composition.ingredients()) {
            Optional<Integer> color = resolveMaterial(ingredient.materialId());
            if (color.isEmpty()) continue;
            int argb = color.orElseThrow();
            long units = ingredient.units();
            red += srgbToLinear((argb >> 16) & 0xFF) * units;
            green += srgbToLinear((argb >> 8) & 0xFF) * units;
            blue += srgbToLinear(argb & 0xFF) * units;
            sampledUnits += units;
        }

        if (sampledUnits == 0) {
            return fallback.map(SourceVisualColorResolver::resolve)
                    .orElse(SourceVisualColorResolver.FALLBACK_ARGB);
        }
        return 0xFF000000
                | (linearToSrgb(red / sampledUnits) << 16)
                | (linearToSrgb(green / sampledUnits) << 8)
                | linearToSrgb(blue / sampledUnits);
    }

    private static Optional<Integer> resolveMaterial(ResourceLocation materialId) {
        try {
            Class<?> gearApi = Class.forName(GEAR_API);
            Object material = gearApi.getMethod("getMaterial", ResourceLocation.class).invoke(null, materialId);
            if (material == null) return Optional.empty();

            Class<?> materialClass = Class.forName(MATERIAL);
            Class<?> partTypeClass = Class.forName(PART_TYPE);
            Object mainPart = partTypeClass.getField("MAIN").get(null);
            Method displayItem = materialClass.getMethod("getDisplayItem", partTypeClass, int.class);
            Object result = displayItem.invoke(material, mainPart, 0);
            if (!(result instanceof ItemStack stack) || stack.isEmpty()) return Optional.empty();
            return SourceVisualIdentity.capture(stack).map(SourceVisualColorResolver::resolve);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static double srgbToLinear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static int linearToSrgb(double value) {
        value = Math.max(0, Math.min(1, value));
        double srgb = value <= 0.0031308 ? value * 12.92 : 1.055 * Math.pow(value, 1 / 2.4) - 0.055;
        return (int) Math.round(srgb * 255);
    }
}
