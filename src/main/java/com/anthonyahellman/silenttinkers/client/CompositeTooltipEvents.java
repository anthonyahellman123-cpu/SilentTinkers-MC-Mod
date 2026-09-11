package com.anthonyahellman.silenttinkers.client;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyDisplayFormatter;
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
            AlloyPayload.readStats(stack).ifPresent(stats -> replaceStaticPartStats(event, stats));
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
        StringBuilder summary = new StringBuilder(AlloyDisplayFormatter.compositionLabel(composition));
        visualSource.flatMap(SourceVisualIdentity::silentGearGrade)
                .ifPresent(grade -> summary.append(" • Grade ").append(grade));
        if (starChargeLevel > 0) summary.append(" • Starcharged ").append(starChargeLevel);
        event.getToolTip().add(Component.literal(summary.toString()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Replaces Tinkers' static synthetic-material label with the encoded alloy's actual identity. */
    private static void applyCompositionLabels(ItemTooltipEvent event,
                                               com.anthonyahellman.silenttinkers.material.AlloyComposition composition) {
        String label = AlloyDisplayFormatter.compositionLabel(composition);
        for (int index = 0; index < event.getToolTip().size(); index++) {
            Component original = event.getToolTip().get(index);
            String text = original.getString();
            if (!text.contains("Composite Alloy")) continue;
            event.getToolTip().set(index, Component.literal(text.replace("Composite Alloy", label))
                    .withStyle(original.getStyle()));
        }
    }

    private static void appendDynamicStats(ItemTooltipEvent event, AlloyStatSnapshot stats) {
        event.getToolTip().add(Component.literal("Encoded dynamic head stats")
                .withStyle(ChatFormatting.YELLOW));
        event.getToolTip().add(Component.literal("Durability: " + stats.durability()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Mining speed: " + stats.miningSpeed()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Melee damage: " + stats.meleeDamage()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Attack speed: " + stats.attackSpeed()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Harvest tier: " + stats.harvestTier()).withStyle(ChatFormatting.GRAY));
    }

    /**
     * Tinkers builds a material-part tooltip from the registered base material before this event fires.
     * Our registered material deliberately contains safe placeholder stats, while the real alloy snapshot
     * lives on the individual cast head. Replace those placeholder lines for normal play instead of only
     * revealing the correct values in the advanced diagnostic section.
     */
    private static void replaceStaticPartStats(ItemTooltipEvent event, AlloyStatSnapshot stats) {
        boolean durability = replaceTooltipLine(event, "Durability:",
                "Durability: " + formatStat(stats.durability()), ChatFormatting.GREEN);
        boolean tier = replaceTooltipLine(event, "Mining Tier:",
                "Mining Tier: " + displayTier(stats.harvestTier()), ChatFormatting.GOLD);
        boolean speed = replaceTooltipLine(event, "Mining Speed:",
                "Mining Speed: " + formatStat(stats.miningSpeed()), ChatFormatting.AQUA);
        boolean damage = replaceTooltipLine(event, "Melee Damage:",
                "Melee Damage: " + formatStat(stats.meleeDamage()), ChatFormatting.RED);

        // Be defensive around Tinkers/add-on tooltip layout changes: if its placeholder block was not
        // present, the encoded values must still be visible rather than silently falling back to Wood/1.
        if (!(durability && tier && speed && damage)) {
            event.getToolTip().add(Component.literal("Head stats").withStyle(ChatFormatting.UNDERLINE));
            if (!durability) event.getToolTip().add(Component.literal(
                    "Durability: " + formatStat(stats.durability())).withStyle(ChatFormatting.GREEN));
            if (!tier) event.getToolTip().add(Component.literal(
                    "Mining Tier: " + displayTier(stats.harvestTier())).withStyle(ChatFormatting.GOLD));
            if (!speed) event.getToolTip().add(Component.literal(
                    "Mining Speed: " + formatStat(stats.miningSpeed())).withStyle(ChatFormatting.AQUA));
            if (!damage) event.getToolTip().add(Component.literal(
                    "Melee Damage: " + formatStat(stats.meleeDamage())).withStyle(ChatFormatting.RED));
        }
    }

    private static boolean replaceTooltipLine(ItemTooltipEvent event, String prefix,
                                              String replacement, ChatFormatting valueColor) {
        for (int index = 0; index < event.getToolTip().size(); index++) {
            if (!event.getToolTip().get(index).getString().trim().startsWith(prefix)) continue;
            event.getToolTip().set(index, Component.literal(replacement).withStyle(valueColor));
            return true;
        }
        return false;
    }

    private static String formatStat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001f) return Integer.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private static String displayTier(net.minecraft.resources.ResourceLocation tier) {
        String path = tier.getPath().replace('_', ' ');
        StringBuilder result = new StringBuilder(path.length());
        boolean capitalize = true;
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
            result.append(capitalize ? Character.toUpperCase(character) : character);
            capitalize = character == ' ';
        }
        return result.toString();
    }
}
