package com.anthonyahellman.silenttinkers;

import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.MaterialDiscoveryState;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
import com.anthonyahellman.silenttinkers.material.UnifiedMaterialDiscovery;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModItems;
import com.anthonyahellman.silenttinkers.registry.ModModifiers;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import slimeknights.tconstruct.tools.TinkerToolParts;

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
        MinecraftForge.EVENT_BUS.addListener(this::onItemTooltip);
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

    /**
     * Tinkers' normal tool-part tooltip reports the static placeholder material
     * stats. Dynamic composite parts carry their real stats in our payload, so
     * expose those directly on the cast pick head to avoid a misleading
     * "wood/1 durability" diagnostic while the part is still unassembled.
     */
    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(TinkerToolParts.pickHead.get())) {
            return;
        }

        AlloyPayload.read(stack).ifPresent(composition -> {
            event.getToolTip().add(Component.literal("SilentTinkers dynamic payload")
                    .withStyle(ChatFormatting.GOLD));
            for (MaterialIngredient ingredient : composition.ingredients()) {
                double percent = 100.0 * ingredient.units() / composition.totalUnits();
                event.getToolTip().add(Component.literal(String.format("%s: %.1f%%", ingredient.materialId(), percent))
                        .withStyle(ChatFormatting.GRAY));
            }
            AlloyPayload.readStats(stack).ifPresent(stats -> appendDynamicStats(event, stats));
        });
    }

    private static void appendDynamicStats(ItemTooltipEvent event, AlloyStatSnapshot stats) {
        event.getToolTip().add(Component.literal("Dynamic head stats (applied on assembled tool)")
                .withStyle(ChatFormatting.YELLOW));
        event.getToolTip().add(Component.literal("Durability: " + stats.durability()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Mining speed: " + stats.miningSpeed()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Melee damage: " + stats.meleeDamage()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Attack speed: " + stats.attackSpeed()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Harvest tier: " + stats.harvestTier()).withStyle(ChatFormatting.GRAY));
    }

    private void onServerStopped(ServerStoppedEvent event) {
        MaterialDiscoveryState.clear();
    }
}
