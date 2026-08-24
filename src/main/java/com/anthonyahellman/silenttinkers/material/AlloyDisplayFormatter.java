package com.anthonyahellman.silenttinkers.material;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** One player-facing vocabulary for fluids, parts, tools, and diagnostics. */
public final class AlloyDisplayFormatter {
    private AlloyDisplayFormatter() {}

    /** Pure: "Elementium". Mixed: "Alloy: Iron 30% + Redstone 70%". */
    public static String compositionLabel(AlloyComposition composition) {
        if (composition.ingredients().size() == 1) {
            return materialName(composition.ingredients().get(0));
        }
        return "Alloy: " + composition.ingredients().stream()
                .map(ingredient -> ingredientLabel(composition, ingredient))
                .collect(Collectors.joining(" + "));
    }

    /** Pure: "Molten Elementium (100%)". Mixed: "Molten Alloy: Iron 30% + Redstone 70%". */
    public static String moltenLabel(AlloyComposition composition) {
        if (composition.ingredients().size() == 1) {
            return "Molten " + materialName(composition.ingredients().get(0)) + " (100%)";
        }
        return "Molten " + compositionLabel(composition);
    }

    public static String ingredientLabel(AlloyComposition composition, MaterialIngredient ingredient) {
        return materialName(ingredient) + " " + formatPercent(percent(composition, ingredient)) + "%";
    }

    public static double percent(AlloyComposition composition, MaterialIngredient ingredient) {
        return 100.0 * ingredient.units() / composition.totalUnits();
    }

    private static String materialName(MaterialIngredient ingredient) {
        String path = ingredient.materialId().getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        return Arrays.stream(path.replace('-', '_').split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static String formatPercent(double percent) {
        return Math.abs(percent - Math.rint(percent)) < 0.05
                ? Long.toString(Math.round(percent))
                : String.format(Locale.ROOT, "%.1f", percent);
    }
}
