package com.anthonyahellman.silenttinkers.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

/** Client-safe molten metal renderer backed by Minecraft's guaranteed lava sprites. */
public final class CompositeAlloyFluidType extends FluidType {
    private static final ResourceLocation STILL_TEXTURE =
            new ResourceLocation("minecraft", "block/lava_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            new ResourceLocation("minecraft", "block/lava_flow");

    public CompositeAlloyFluidType(Properties properties) {
        super(properties);
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
                return 0xFFB768FF;
            }
        });
    }
}
