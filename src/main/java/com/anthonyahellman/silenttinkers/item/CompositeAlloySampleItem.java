package com.anthonyahellman.silenttinkers.item;

import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A diagnostic ingot-shaped carrier proving a composition survived casting. */
public final class CompositeAlloySampleItem extends Item {
    public CompositeAlloySampleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        AlloyPayload.read(stack).ifPresent(composition -> {
            tooltip.add(Component.translatable("tooltip.silenttinkers.fingerprint", composition.fingerprint())
                    .withStyle(ChatFormatting.DARK_GRAY));
            for (MaterialIngredient ingredient : composition.ingredients()) {
                double percent = 100.0 * ingredient.units() / composition.totalUnits();
                tooltip.add(Component.literal(String.format("%s: %.1f%%", ingredient.materialId(), percent))
                        .withStyle(ChatFormatting.GRAY));
            }
            AlloyPayload.readVisualSource(stack).ifPresent(visual -> tooltip.add(
                    Component.literal("Visual source: " + visual.itemId()
                                    + (visual.itemTag().isPresent() ? " (dynamic tag preserved)" : ""))
                            .withStyle(ChatFormatting.AQUA)));
            int starChargeLevel = AlloyPayload.readStarChargeLevel(stack);
            if (starChargeLevel > 0) {
                tooltip.add(Component.translatable("tooltip.silenttinkers.starcharged", starChargeLevel)
                        .withStyle(ChatFormatting.AQUA));
            }
            AlloyPayload.readStats(stack).ifPresent(stats -> appendStats(tooltip, stats));
        });
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static void appendStats(List<Component> tooltip, AlloyStatSnapshot stats) {
        tooltip.add(Component.translatable("tooltip.silenttinkers.evaluated_stats")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.silenttinkers.durability", stats.durability())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.silenttinkers.mining_speed", stats.miningSpeed())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.silenttinkers.melee_damage", stats.meleeDamage())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.silenttinkers.attack_speed", stats.attackSpeed())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.silenttinkers.harvest_tier", stats.harvestTier())
                .withStyle(ChatFormatting.GRAY));
    }
}
