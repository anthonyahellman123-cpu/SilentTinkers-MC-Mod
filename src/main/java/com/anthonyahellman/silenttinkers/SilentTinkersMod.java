package com.anthonyahellman.silenttinkers;

import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;
import com.anthonyahellman.silenttinkers.material.MaterialDiscoveryState;
import com.anthonyahellman.silenttinkers.material.UnifiedMaterialDiscovery;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModItems;
import com.anthonyahellman.silenttinkers.registry.ModModifiers;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
        ModModifiers.MODIFIERS.register(modBus);

        MinecraftForge.EVENT_BUS.addListener(this::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("Silent Tinkers compatibility bridge loaded");
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null && MaterialDiscoveryState.current().isPresent()) {
            return;
        }

        LOGGER.info("SilentTinkers material scan starting after datapack sync");
        try {
            UnifiedMaterialDiscovery.discover();
        } catch (RuntimeException | LinkageError exception) {
            // A bad addon/material must never make player login or a dedicated
            // server datapack sync fail. Drop the snapshot so the bridge cannot
            // act on stale or partial data; later syncs may safely retry.
            MaterialDiscoveryState.clear();
            LOGGER.error("[SilentTinkers:SCAN_FAILED] Material discovery failed; automatic bridging disabled until a later successful scan", exception);
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        MaterialDiscoveryState.clear();
    }
}
