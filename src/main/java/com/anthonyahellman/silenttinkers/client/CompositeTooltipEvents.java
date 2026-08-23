package com.anthonyahellman.silenttinkers.client;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerToolParts;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only development diagnostics for synthetic composite parts and tools.
 * Keeping tooltip code out of common mod initialization also guarantees the
 * dedicated-server path never has to load this class.
 */
@Mod.EventBusSubscriber(modid = SilentTinkersMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompositeTooltipEvents {
    private static final Set<String> LOGGED_TOOL_OBSERVATIONS = ConcurrentHashMap.newKeySet();

    private CompositeTooltipEvents() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
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
            AlloyPayload.readVisualSource(stack).ifPresent(visual -> event.getToolTip().add(
                    Component.literal("Visual source: " + visual.itemId()
                                    + (visual.itemTag().isPresent() ? " (dynamic tag preserved)" : ""))
                            .withStyle(ChatFormatting.AQUA)));
            AlloyPayload.readStats(stack).ifPresent(stats -> appendDynamicStats(event, stats));
        });
    }

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
                .anyMatch(entry -> entry.getId().toString().equals(SilentTinkersMod.MOD_ID + ":composite_alloy"));
        event.getToolTip().add(Component.literal("Composite modifier: " + (modifierPresent ? "BOUND" : "MISSING"))
                .withStyle(modifierPresent ? ChatFormatting.GREEN : ChatFormatting.RED));

        String variant = composite.getVariant().getVariant();
        Optional<AlloyStatSnapshot> encodedStats = Optional.empty();
        try {
            encodedStats = AlloyVariantCodec.decodeStats(variant);
            if (encodedStats.isPresent()) {
                event.getToolTip().add(Component.literal("Variant stats: PRESENT")
                        .withStyle(ChatFormatting.GREEN));
                AlloyStatSnapshot stats = encodedStats.orElseThrow();
                event.getToolTip().add(Component.literal(
                                "Encoded head: durability " + stats.durability()
                                        + ", speed " + stats.miningSpeed()
                                        + ", damage " + stats.meleeDamage()
                                        + ", attack speed " + stats.attackSpeed()
                                        + ", tier " + stats.harvestTier())
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                event.getToolTip().add(Component.literal("Variant stats: MISSING")
                        .withStyle(ChatFormatting.RED));
            }
        } catch (IllegalArgumentException exception) {
            event.getToolTip().add(Component.literal("Variant stats: INVALID")
                    .withStyle(ChatFormatting.RED));
        }

        float finalDurability = tool.getStats().get(ToolStats.DURABILITY);
        float finalMiningSpeed = tool.getStats().get(ToolStats.MINING_SPEED);
        float finalMeleeDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        float finalAttackSpeed = tool.getStats().get(ToolStats.ATTACK_SPEED);
        Object finalHarvestTier = tool.getStats().get(ToolStats.HARVEST_TIER);

        event.getToolTip().add(Component.literal("Final durability: " + finalDurability)
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final mining speed: " + finalMiningSpeed)
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final melee damage: " + finalMeleeDamage)
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final attack speed: " + finalAttackSpeed)
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final harvest tier: " + finalHarvestTier)
                .withStyle(ChatFormatting.GRAY));

        String observationKey = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + composite.getVariant();
        if (LOGGED_TOOL_OBSERVATIONS.add(observationKey)) {
            SilentTinkersMod.LOGGER.info(
                    "[SilentTinkers:COMPOSITE_TOOL_OBSERVED] tool={} variant={} modifierPresent={} encodedStatsPresent={} finalDurability={} finalMiningSpeed={} finalMeleeDamage={} finalAttackSpeed={} finalHarvestTier={}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()), composite.getVariant(), modifierPresent,
                    encodedStats.isPresent(), finalDurability, finalMiningSpeed, finalMeleeDamage,
                    finalAttackSpeed, finalHarvestTier);
        }
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
}
