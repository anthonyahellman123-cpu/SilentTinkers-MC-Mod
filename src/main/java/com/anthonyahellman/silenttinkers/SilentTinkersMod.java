package com.anthonyahellman.silenttinkers;

import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.material.MaterialDiscoveryState;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
import com.anthonyahellman.silenttinkers.material.UnifiedMaterialDiscovery;
import com.anthonyahellman.silenttinkers.modifier.CompositeAlloyModifier;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
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
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerToolParts;
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
            validateCompositeTraitBinding();
        } catch (RuntimeException | LinkageError exception) {
            MaterialDiscoveryState.clear();
            LOGGER.error("[SilentTinkers:SCAN_FAILED] Material discovery failed; automatic bridging disabled until a later successful scan", exception);
        }
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
            LOGGER.error("[SilentTinkers:COMPOSITE_TRAIT_MISSING] material={} statType={} traits={} -- assembled tools cannot receive dynamic stats until this trait is present",
                    CompositePickHeadCastingRecipe.MATERIAL, HeadMaterialStats.ID, traits);
            return;
        }

        Object modifier = entry.getModifier();
        ToolStatsModifierHook statsHook = entry.getHook(ModifierHooks.TOOL_STATS);
        boolean modifierClassOk = modifier instanceof CompositeAlloyModifier;
        boolean hookClassOk = statsHook instanceof CompositeAlloyModifier;
        if (modifierClassOk && hookClassOk) {
            LOGGER.info("[SilentTinkers:COMPOSITE_TRAIT_BOUND] material={} statType={} modifierClass={} hookClass={} traits={}",
                    CompositePickHeadCastingRecipe.MATERIAL, HeadMaterialStats.ID,
                    modifier.getClass().getName(), statsHook.getClass().getName(), traits);
        } else {
            LOGGER.error("[SilentTinkers:COMPOSITE_HOOK_MISMATCH] material={} statType={} modifierClass={} hookClass={} traits={} -- trait ID exists but does not resolve to the expected tool-stat hook",
                    CompositePickHeadCastingRecipe.MATERIAL, HeadMaterialStats.ID,
                    modifier.getClass().getName(), statsHook.getClass().getName(), traits);
        }
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(TinkerToolParts.pickHead.get())) {
            appendCompositePartTooltip(event, stack);
            return;
        }
        if (stack.getItem() instanceof IModifiable) {
            appendCompositeToolTooltip(event, stack);
        }
    }

    private static void appendCompositePartTooltip(ItemTooltipEvent event, ItemStack stack) {
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

    /**
     * Development-facing end-to-end checkpoint. If a finished Tinkers tool
     * contains our composite material, show whether the encoded variant survived,
     * whether the composite trait was actually copied onto the tool, and the
     * final stats Tinkers stored after its rebuild pipeline.
     */
    private static void appendCompositeToolTooltip(ItemTooltipEvent event, ItemStack stack) {
        ToolStack tool = ToolStack.from(stack);
        MaterialVariant composite = null;
        for (MaterialVariant material : tool.getMaterials()) {
            if (material.getVariant().getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) {
                composite = material;
                break;
            }
        }
        if (composite == null) {
            return;
        }

        event.getToolTip().add(Component.literal("SilentTinkers assembled-tool diagnostic")
                .withStyle(ChatFormatting.GOLD));

        boolean modifierPresent = tool.getModifiers().getModifiers().stream()
                .anyMatch(entry -> entry.getId().toString().equals(MOD_ID + ":composite_alloy"));
        event.getToolTip().add(Component.literal("Composite modifier: " + (modifierPresent ? "BOUND" : "MISSING"))
                .withStyle(modifierPresent ? ChatFormatting.GREEN : ChatFormatting.RED));

        String variant = composite.getVariant().getVariant();
        try {
            AlloyVariantCodec.decodeStats(variant).ifPresentOrElse(
                    stats -> event.getToolTip().add(Component.literal("Variant stats: PRESENT")
                            .withStyle(ChatFormatting.GREEN)),
                    () -> event.getToolTip().add(Component.literal("Variant stats: MISSING")
                            .withStyle(ChatFormatting.RED)));
        } catch (IllegalArgumentException exception) {
            event.getToolTip().add(Component.literal("Variant stats: INVALID")
                    .withStyle(ChatFormatting.RED));
        }

        event.getToolTip().add(Component.literal("Final durability: " + tool.getStats().get(ToolStats.DURABILITY))
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final mining speed: " + tool.getStats().get(ToolStats.MINING_SPEED))
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final melee damage: " + tool.getStats().get(ToolStats.ATTACK_DAMAGE))
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final attack speed: " + tool.getStats().get(ToolStats.ATTACK_SPEED))
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final harvest tier: " + tool.getStats().get(ToolStats.HARVEST_TIER))
                .withStyle(ChatFormatting.GRAY));
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
