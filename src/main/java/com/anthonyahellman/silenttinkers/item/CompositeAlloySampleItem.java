package com.anthonyahellman.silenttinkers.item;

import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
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
        });
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
