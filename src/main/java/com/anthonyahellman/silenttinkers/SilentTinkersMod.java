package com.anthonyahellman.silenttinkers;

import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModItems;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SilentTinkersMod.MOD_ID)
public final class SilentTinkersMod {
    public static final String MOD_ID = "silenttinkers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SilentTinkersMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SilentTinkersConfig.SPEC);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        LOGGER.info("Silent Tinkers compatibility bridge loaded");
    }
}
