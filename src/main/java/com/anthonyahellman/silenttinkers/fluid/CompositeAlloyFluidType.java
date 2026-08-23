package com.anthonyahellman.silenttinkers.fluid;

import com.anthonyahellman.silenttinkers.client.AlloyVisualColorResolver;
import com.anthonyahellman.silenttinkers.client.SourceVisualColorResolver;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import java.util.Locale;
import java.util.function.Consumer;

/** Dynamic molten alloy name and client renderer backed by Tinkers' neutral molten sprites. */
public final class CompositeAlloyFluidType extends FluidType {
    private static final ResourceLocation STILL_TEXTURE =
            new ResourceLocation("tconstruct", "fluid/molten/still");
    private static final ResourceLocation FLOWING_TEXTURE =
            new ResourceLocation("tconstruct", "fluid/molten/flowing");

    public CompositeAlloyFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public Component getDescription(FluidStack stack) {
        return AlloyPayload.read(stack).map(composition -> {
            if (composition.ingredients().size() == 1) {
                return Component.literal("Molten " + displayName(composition.ingredients().get(0)) + " (100%)");
            }
            if (composition.ingredients().size() <= 3) {
                String shares = composition.ingredients().stream()
                        .map(ingredient -> displayName(ingredient) + " " + String.format(Locale.ROOT, "%.1f%%",
                                100.0 * ingredient.units() / composition.totalUnits()))
                        .reduce((left, right) -> left + " / " + right)
                        .orElse("Composite");
                return Component.literal("Molten Alloy (" + shares + ")");
            }
            return Component.literal("Molten Composite Alloy (" + composition.ingredients().size() + " Materials)");
        }).orElseGet(() -> super.getDescription(stack));
    }

    private static String displayName(MaterialIngredient ingredient) {
        String path = ingredient.materialId().getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        StringBuilder result = new StringBuilder(path.length());
        boolean capitalize = true;
        for (char character : path.toCharArray()) {
            if (character == '_') {
                result.append(' ');
                capitalize = true;
            } else {
                result.append(capitalize ? Character.toUpperCase(character) : character);
                capitalize = false;
            }
        }
        return result.toString();
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return SourceVisualColorResolver.FALLBACK_ARGB;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return AlloyPayload.read(stack)
                        .map(composition -> AlloyVisualColorResolver.resolve(
                                composition, AlloyPayload.readVisualSource(stack)))
                        .orElse(SourceVisualColorResolver.FALLBACK_ARGB);
            }
        });
    }
}
