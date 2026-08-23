package com.anthonyahellman.silenttinkers;

import com.anthonyahellman.silenttinkers.command.SilentTinkersCommands;
import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;
import com.anthonyahellman.silenttinkers.material.CompositeBridgeHealth;
import com.anthonyahellman.silenttinkers.material.MaterialDiscoveryState;
import com.anthonyahellman.silenttinkers.material.RuntimeBridgeHealth;
import com.anthonyahellman.silenttinkers.material.StarChargeBridgeHealth;
import com.anthonyahellman.silenttinkers.material.UnifiedMaterialDiscovery;
import com.anthonyahellman.silenttinkers.modifier.CompositeAlloyModifier;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModItems;
import com.anthonyahellman.silenttinkers.registry.ModModifiers;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.List;

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
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("Silent Tinkers compatibility bridge loaded");
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null && MaterialDiscoveryState.current().isPresent()) return;

        LOGGER.info("SilentTinkers material scan starting after datapack sync");
        try {
            UnifiedMaterialDiscovery.discover();
            validateCompositeTraitBinding();
        } catch (RuntimeException | LinkageError exception) {
            MaterialDiscoveryState.clear();
            CompositeBridgeHealth.clear();
            RuntimeBridgeHealth.clear();
            StarChargeBridgeHealth.clear();
            LOGGER.error("[SilentTinkers:SCAN_FAILED] Material discovery failed; automatic bridging disabled until a later successful scan", exception);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        SilentTinkersCommands.register(event);
    }

    private static void validateCompositeTraitBinding() {
        List<ModifierEntry> traits = MaterialRegistry.getInstance()
                .getTraits(CompositePickHeadCastingRecipe.MATERIAL, HeadMaterialStats.ID);
        String expected = MOD_ID + ":composite_alloy";
        ModifierEntry entry = traits.stream()
                .filter(candidate -> candidate.getId().toString().equals(expected))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            CompositeBridgeHealth.set(CompositeBridgeHealth.Status.TRAIT_MISSING);
            LOGGER.error("[SilentTinkers:COMPOSITE_TRAIT_MISSING] material={} statType={} traits={} -- assembled tools cannot receive dynamic stats until this trait is present",
                    CompositePickHeadCastingRecipe.MATERIAL, HeadMaterialStats.ID, traits);
            return;
        }

        Object modifier = entry.getModifier();
        ToolStatsModifierHook statsHook = entry.getHook(ModifierHooks.TOOL_STATS);
        boolean modifierClassOk = modifier instanceof CompositeAlloyModifier;
        boolean hookClassOk = statsHook instanceof CompositeAlloyModifier;
        if (modifierClassOk && hookClassOk) {
            CompositeBridgeHealth.set(CompositeBridgeHealth.Status.BOUND);
            LOGGER.info("[SilentTinkers:COMPOSITE_TRAIT_BOUND] material={} statType={} modifierClass={} hookClass={} traits={}",
                    CompositePickHeadCastingRecipe.MATERIAL, HeadMaterialStats.ID,
                    modifier.getClass().getName(), statsHook.getClass().getName(), traits);
        } else {
            CompositeBridgeHealth.set(CompositeBridgeHealth.Status.HOOK_MISMATCH);
            LOGGER.error("[SilentTinkers:COMPOSITE_HOOK_MISMATCH] material={} statType={} modifierClass={} hookClass={} traits={} -- trait ID exists but does not resolve to the expected tool-stat hook",
                    CompositePickHeadCastingRecipe.MATERIAL, HeadMaterialStats.ID,
                    modifier.getClass().getName(), statsHook.getClass().getName(), traits);
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        MaterialDiscoveryState.clear();
        CompositeBridgeHealth.clear();
        RuntimeBridgeHealth.clear();
        StarChargeBridgeHealth.clear();
    }
}
