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

        // Do not scan from AddReloadListenerEvent. Our first pack test proved our
        // listener's apply() ran before Silent Gear/Tinkers had published their
        // own datapack-backed material registries. Datapack sync occurs after the
        // reload is complete and is therefore a safe observation point.
        MinecraftForge.EVENT_BUS.addListener(this::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("Silent Tinkers compatibility bridge loaded");
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        // player == null means a global sync after a reload. A non-null player is
        // normally a login sync; avoid rescanning the whole material graph for
        // every player joining once a valid snapshot already exists.
        if (event.getPlayer() == null || MaterialDiscoveryState.current().isEmpty()) {
            LOGGER.info("SilentTinkers material scan starting after datapack sync");
            UnifiedMaterialDiscovery.discover();
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        MaterialDiscoveryState.clear();
    }
}
