package com.anthonyahellman.silenttinkers.registry;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, SilentTinkersMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, SilentTinkersMod.MOD_ID);

    public static final RegistryObject<FluidType> COMPOSITE_ALLOY_TYPE = FLUID_TYPES.register(
            "molten_composite_alloy",
            () -> new FluidType(FluidType.Properties.create()
                    .density(3000).viscosity(6000).temperature(1500)
                    .sound(SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA)
                    .sound(SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_LAVA)) {});

    public static final RegistryObject<FlowingFluid> MOLTEN_COMPOSITE_ALLOY = FLUIDS.register(
            "molten_composite_alloy", () -> new ForgeFlowingFluid.Source(properties()));
    public static final RegistryObject<FlowingFluid> FLOWING_COMPOSITE_ALLOY = FLUIDS.register(
            "flowing_molten_composite_alloy", () -> new ForgeFlowingFluid.Flowing(properties()));

    private static ForgeFlowingFluid.Properties properties() {
        return new ForgeFlowingFluid.Properties(
                COMPOSITE_ALLOY_TYPE, MOLTEN_COMPOSITE_ALLOY, FLOWING_COMPOSITE_ALLOY)
                .slopeFindDistance(2).levelDecreasePerBlock(2);
    }

    private ModFluids() {}
}
