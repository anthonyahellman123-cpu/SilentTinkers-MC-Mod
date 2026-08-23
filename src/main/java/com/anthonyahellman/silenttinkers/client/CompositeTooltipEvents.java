package com.anthonyahellman.silenttinkers.client;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
import com.anthonyahellman.silenttinkers.material.SourceVisualIdentity;
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
import java.util.stream.Collectors;

/** Client-only development diagnostics for synthetic composite parts and tools. */
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
        if (stack.getItem() instanceof IModifiable) appendCompositeToolTooltip(event, stack);
    }

    private static void appendCompositePartTooltip(ItemTooltipEvent event, ItemStack stack) {
        AlloyPayload.read(stack).ifPresent(composition -> {
            applyCompositionLabels(event, composition);
            appendPlayerSummary(event, AlloyPayload.readVisualSource(stack),
                    AlloyPayload.readStarChargeLevel(stack), composition);
            if (!event.getFlags().isAdvanced()) return;

            event.getToolTip().add(Component.literal("SilentTinkers diagnostics").withStyle(ChatFormatting.DARK_GRAY));
            for (MaterialIngredient ingredient : composition.ingredients()) {
                double percent = 100.0 * ingredient.units() / composition.totalUnits();
                event.getToolTip().add(Component.literal(String.format("%s: %.1f%%", ingredient.materialId(), percent))
                        .withStyle(ChatFormatting.GRAY));
            }
            AlloyPayload.readVisualSource(stack).ifPresent(visual -> {
                event.getToolTip().add(Component.literal("Visual source: " + visual.itemId()
                                + (visual.itemTag().isPresent() ? " (dynamic tag preserved)" : ""))
                        .withStyle(ChatFormatting.AQUA));
                visual.silentGearGrade().ifPresent(grade -> event.getToolTip().add(
                        Component.literal("Silent Gear grade: " + grade).withStyle(ChatFormatting.LIGHT_PURPLE)));
                int mixedColor = AlloyVisualColorResolver.resolve(composition, Optional.of(visual));
                event.getToolTip().add(Component.literal("Mixed alloy color: " + hexColor(mixedColor))
                        .withStyle(ChatFormatting.DARK_AQUA));
            });
            AlloyPayload.readStats(stack).ifPresent(stats -> appendDynamicStats(event, stats));
            int starChargeLevel = AlloyPayload.readStarChargeLevel(stack);
            event.getToolTip().add(Component.literal("Starcharge: "
                            + (starChargeLevel > 0 ? "level " + starChargeLevel : "uncharged"))
                    .withStyle(starChargeLevel > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
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
        if (composite == null) return;

        String variant = composite.getVariant().getVariant();
        try {
            var composition = AlloyVariantCodec.decode(variant);
            applyCompositionLabels(event, composition);
            appendPlayerSummary(event, AlloyVariantCodec.decodeVisualSource(variant),
                    AlloyVariantCodec.decodeStarChargeLevel(variant),
                    composition);
        } catch (IllegalArgumentException exception) {
            event.getToolTip().add(Component.literal("SilentTinkers payload invalid").withStyle(ChatFormatting.RED));
        }
        if (!event.getFlags().isAdvanced()) return;

        event.getToolTip().add(Component.literal("SilentTinkers diagnostics").withStyle(ChatFormatting.DARK_GRAY));

        boolean modifierPresent = tool.getModifiers().getModifiers().stream()
                .anyMatch(entry -> entry.getId().toString().equals(SilentTinkersMod.MOD_ID + ":composite_alloy"));
        event.getToolTip().add(Component.literal("Composite modifier: " + (modifierPresent ? "BOUND" : "MISSING"))
                .withStyle(modifierPresent ? ChatFormatting.GREEN : ChatFormatting.RED));

        Optional<AlloyStatSnapshot> encodedStats = Optional.empty();
        Optional<SourceVisualIdentity> visualSource = Optional.empty();
        int visualColor = SourceVisualColorResolver.FALLBACK_ARGB;
        try {
            int starChargeLevel = AlloyVariantCodec.decodeStarChargeLevel(variant);
            event.getToolTip().add(Component.literal("Starcharge: "
                            + (starChargeLevel > 0 ? "level " + starChargeLevel : "uncharged"))
                    .withStyle(starChargeLevel > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
            encodedStats = AlloyVariantCodec.decodeStats(variant);
            visualSource = AlloyVariantCodec.decodeVisualSource(variant);
            Optional<SourceVisualIdentity> observedVisualSource = visualSource;
            event.getToolTip().add(Component.literal("Visual source: "
                            + observedVisualSource.map(SourceVisualIdentity::itemId).map(Object::toString).orElse("NOT ENCODED")
                            + observedVisualSource.filter(source -> source.itemTag().isPresent())
                                    .map(source -> " (dynamic tag encoded)").orElse(""))
                    .withStyle(observedVisualSource.isPresent() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
            if (visualSource.isPresent()) {
                visualSource.orElseThrow().silentGearGrade().ifPresent(grade -> event.getToolTip().add(
                        Component.literal("Silent Gear grade: " + grade).withStyle(ChatFormatting.LIGHT_PURPLE)));
                visualColor = AlloyVisualColorResolver.resolve(
                        AlloyVariantCodec.decode(variant), visualSource);
                event.getToolTip().add(Component.literal("Mixed alloy color: " + hexColor(visualColor))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
            if (encodedStats.isPresent()) {
                event.getToolTip().add(Component.literal("Variant stats: PRESENT").withStyle(ChatFormatting.GREEN));
                AlloyStatSnapshot stats = encodedStats.orElseThrow();
                event.getToolTip().add(Component.literal(
                                "Encoded head: durability " + stats.durability()
                                        + ", speed " + stats.miningSpeed()
                                        + ", damage " + stats.meleeDamage()
                                        + ", attack speed " + stats.attackSpeed()
                                        + ", tier " + stats.harvestTier())
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                event.getToolTip().add(Component.literal("Variant stats: MISSING").withStyle(ChatFormatting.RED));
            }
        } catch (IllegalArgumentException exception) {
            event.getToolTip().add(Component.literal("Variant payload: INVALID").withStyle(ChatFormatting.RED));
        }

        float finalDurability = tool.getStats().get(ToolStats.DURABILITY);
        float finalMiningSpeed = tool.getStats().get(ToolStats.MINING_SPEED);
        float finalMeleeDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        float finalAttackSpeed = tool.getStats().get(ToolStats.ATTACK_SPEED);
        Object finalHarvestTier = tool.getStats().get(ToolStats.HARVEST_TIER);

        event.getToolTip().add(Component.literal("Final durability: " + finalDurability).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final mining speed: " + finalMiningSpeed).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final melee damage: " + finalMeleeDamage).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final attack speed: " + finalAttackSpeed).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Final harvest tier: " + finalHarvestTier).withStyle(ChatFormatting.GRAY));

        String observationKey = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + composite.getVariant();
        if (LOGGED_TOOL_OBSERVATIONS.add(observationKey)) {
            SilentTinkersMod.LOGGER.info(
                    "[SilentTinkers:COMPOSITE_TOOL_OBSERVED] tool={} variant={} modifierPresent={} encodedStatsPresent={} visualSource={} visualTagPresent={} visualColor={} finalDurability={} finalMiningSpeed={} finalMeleeDamage={} finalAttackSpeed={} finalHarvestTier={}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()), composite.getVariant(), modifierPresent,
                    encodedStats.isPresent(), visualSource.map(SourceVisualIdentity::itemId).map(Object::toString).orElse("NONE"),
                    visualSource.filter(source -> source.itemTag().isPresent()).isPresent(), hexColor(visualColor),
                    finalDurability, finalMiningSpeed, finalMeleeDamage, finalAttackSpeed, finalHarvestTier);
        }
    }

    private static String hexColor(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    /** Compact information intended for normal play; F3+H reveals the full diagnostic block. */
    private static void appendPlayerSummary(ItemTooltipEvent event, Optional<SourceVisualIdentity> visualSource,
                                            int starChargeLevel, com.anthonyahellman.silenttinkers.material.AlloyComposition composition) {
        StringBuilder summary = new StringBuilder(compositionLabel(composition));
        visualSource.flatMap(SourceVisualIdentity::silentGearGrade)
                .ifPresent(grade -> summary.append(" • Grade ").append(grade));
        if (starChargeLevel > 0) summary.append(" • Starcharged ").append(starChargeLevel);
        event.getToolTip().add(Component.literal(summary.toString()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Replaces Tinkers' static synthetic-material label with the encoded alloy's actual identity. */
    private static void applyCompositionLabels(ItemTooltipEvent event,
                                               com.anthonyahellman.silenttinkers.material.AlloyComposition composition) {
        String label = compositionLabel(composition);
        for (int index = 0; index < event.getToolTip().size(); index++) {
            Component original = event.getToolTip().get(index);
            String text = original.getString();
            if (!text.contains("Composite Alloy")) continue;
            event.getToolTip().set(index, Component.literal(text.replace("Composite Alloy", label))
                    .withStyle(original.getStyle()));
        }
    }

    private static String compositionLabel(
            com.anthonyahellman.silenttinkers.material.AlloyComposition composition) {
        if (composition.ingredients().size() == 1) {
            return humanize(composition.ingredients().get(0).materialId().getPath());
        }
        return "Alloy: " + composition.ingredients().stream()
                .map(ingredient -> humanize(ingredient.materialId().getPath()) + " "
                        + formatPercent(100.0 * ingredient.units() / composition.totalUnits()) + "%")
                .collect(Collectors.joining(" + "));
    }

    private static String humanize(String path) {
        return java.util.Arrays.stream(path.replace('-', '_').split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static String formatPercent(double percent) {
        return Math.abs(percent - Math.rint(percent)) < 0.05
                ? Long.toString(Math.round(percent))
                : String.format(java.util.Locale.ROOT, "%.1f", percent);
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
