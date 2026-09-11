package com.anthonyahellman.silenttinkers.fluid;

import com.anthonyahellman.silenttinkers.client.AlloyVisualColorResolver;
import com.anthonyahellman.silenttinkers.client.SourceVisualColorResolver;
import com.anthonyahellman.silenttinkers.material.AlloyDisplayFormatter;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

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
        return AlloyPayload.read(stack)
                .<Component>map(composition -> Component.literal(AlloyDisplayFormatter.moltenLabel(composition)))
                .orElseGet(() -> super.getDescription(stack));
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
