package com.anthonyahellman.silenttinkers.modifier;

import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.TierSortingRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Optional;

/** Replaces the composite head's placeholder contribution with Silent Gear's evaluated stats. */
public final class CompositeAlloyModifier extends Modifier implements ToolStatsModifierHook {
    private static final float PLACEHOLDER_DURABILITY = 250.0f;
    private static final float PLACEHOLDER_MINING_SPEED = 6.0f;
    private static final float PLACEHOLDER_MELEE_DAMAGE = 2.0f;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        for (MaterialVariant material : context.getMaterials()) {
            if (!material.getVariant().getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) {
                continue;
            }
            Optional<AlloyStatSnapshot> decoded;
            try {
                decoded = AlloyVariantCodec.decodeStats(material.getVariant().getVariant());
            } catch (IllegalArgumentException exception) {
                continue;
            }
            decoded.ifPresent(stats -> apply(stats, builder));
        }
    }

    private static void apply(AlloyStatSnapshot stats, ModifierStatsBuilder builder) {
        ToolStats.DURABILITY.add(builder, stats.durability() - PLACEHOLDER_DURABILITY);
        ToolStats.MINING_SPEED.add(builder, stats.miningSpeed() - PLACEHOLDER_MINING_SPEED);
        ToolStats.ATTACK_DAMAGE.add(builder, stats.meleeDamage() - PLACEHOLDER_MELEE_DAMAGE);
        ToolStats.ATTACK_SPEED.add(builder, stats.attackSpeed());

        Tier tier = TierSortingRegistry.byName(stats.harvestTier());
        if (tier != null) {
            ToolStats.HARVEST_TIER.update(builder, tier);
        }
    }
}
